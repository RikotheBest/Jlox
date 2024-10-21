import java.util.List;
public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    private Expr expression(){
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator,right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL, TokenType.LESS)){
            Token operand = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operand, right);
        }
        return expr;
    }

    private Expr term() {

        return null;
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









/*
expression → equality ;
equality → comparison ( ( "!=" | "==" ) comparison )* ;
comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term → factor ( ( "-" | "+" ) factor )* ;
factor → unary ( ( "/" | "*" ) unary )* ;
unary → ( "!" | "-" ) unary | primary ;
primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
 */