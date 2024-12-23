import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Arrays;


public class Parser {
    public class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()){
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration(){
        try{
            if(match(TokenType.CLASS)) return classDeclaration();
            if(match(TokenType.VAR)) return varDeclaration();
            if(match(TokenType.FUN)) return function("function");
            return statement();
        }catch (ParseError error){
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration(){
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if(match(TokenType.LESS)){
            consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(previous());
        }
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            methods.add(function("method"));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods, superclass);
    }

    private Stmt varDeclaration() {
         Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

         Expr initializer = null;
         if(match(TokenType.EQUAL)){
             initializer = expression();
         }
         consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
         return new Stmt.Var(name, initializer);
    }
    private Stmt.Function function(String kind){
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if(!check(TokenType.RIGHT_PAREN)){
            do {
                if(parameters.size() >= 255){
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER,"Expect parameter name."));
            }while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN,"Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name,parameters,body);
    }


    private Stmt statement() {
        if(match(TokenType.PRINT)) return printStatement();
        if(match(TokenType.WHILE)) return whileStatement();
        if(match(TokenType.FOR)) return forStatement();
        if(match(TokenType.IF)) return ifStatement();
        if(match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        if(match(TokenType.RETURN)) return returnStatement();
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token returnStmt = previous();
        Expr expr = null;
        if(!check(TokenType.SEMICOLON)){
            expr = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(returnStmt, expr);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if(match(TokenType.SEMICOLON)){
            initializer = null;
        }else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        }else{
            initializer = expressionStatement();
        }

        Expr condition = null;
        if(!check(TokenType.SEMICOLON)){
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");
        Expr increment = null;
        if(!check(TokenType.RIGHT_PAREN)){
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'for' clauses.");
        Stmt body = statement();

        if(increment != null){
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if(condition == null){
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);
        if(initializer != null){
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;


    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect a '(' after while");
        Expr expr = expression();
        consume(TokenType.RIGHT_PAREN, "Expect a ')' after a while condition");
        Stmt body = statement();
        return new Stmt.While(expr, body);
    }

    private Stmt ifStatement(){
        consume(TokenType.LEFT_PAREN, "Expect a '(' after if");
        Expr expr = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(TokenType.ELSE)){
            elseBranch = statement();
        }
        return new Stmt.If(expr, thenBranch, elseBranch);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE,"Expect '}' after block.");
        return statements;
    }

    private Expr expression(){
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if(match(TokenType.EQUAL)){
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }else if (expr instanceof Expr.Get){
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }
    private Expr or(){
        return parseBinaryOp(this::and, TokenType.OR);
    }
    private Expr and(){
        return parseBinaryOp(this::equality, TokenType.AND);
    }

    private Expr parseBinaryOp (Supplier<Expr> supplier, TokenType... types) {
        Expr expr = supplier.get();
        while(match(types)){
            Token operator = previous();
            Expr right = supplier.get();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
       return parseBinaryOp(this::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL);
    }

    private Expr comparison() {
        return parseBinaryOp(this::term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL, TokenType.LESS);
    }

    private Expr term() {
        return parseBinaryOp(this::factor, TokenType.MINUS, TokenType.PLUS);
    }

    private Expr factor() {
        return parseBinaryOp(this::unary, TokenType.SLASH,TokenType.STAR);
    }

    private Expr unary() {
        if(match(TokenType.BANG, TokenType.MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator,right);
        }
        return call();
    }

    private Expr call(){
        Expr expr = primary();
        while (true){
            if(match(TokenType.LEFT_PAREN)){
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if(!check(TokenType.RIGHT_PAREN)){
            do {
                if(arguments.size() >= 255) error(peek(), "Can't have more than 255 arguments.");
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if(match(TokenType.FALSE)) return new Expr.Literal(false);
        if(match(TokenType.TRUE)) return new Expr.Literal(true);
        if(match(TokenType.NIL)) return new Expr.Literal(null);

        if(match(TokenType.NUMBER, TokenType.STRING)){
            return new Expr.Literal(previous().literal);
        }
        if(match(TokenType.IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if(match(TokenType.LEFT_PAREN)){
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if(match(TokenType.SUPER)){
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }
        if(match(TokenType.THIS)) return new Expr.This(previous());
        throw error(peek(), "Expect expression");
    }

    private Token consume(TokenType type, String message) throws ParseError {
        if(check(type)) return advance();
        throw error(peek(), message);
    }
    private ParseError error(Token token, String message){
        Main.error(token, message);
        return new ParseError();
    }
    private void synchronize(){
        advance();
        while(!isAtEnd()){
            if(previous().type == TokenType.SEMICOLON) return;

            switch (peek().type){
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private boolean match(TokenType... types){
        for(TokenType type : types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }
    private boolean check(TokenType type){
        if(isAtEnd()) return false;
        return peek().type == type;
    }
    private Token advance(){
        if(!isAtEnd()) current++;
        return previous();
    }
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
    private Token peek() {
        return tokens.get(current);
    }
    private Token previous() {
        return tokens.get(current - 1);
    }
}








