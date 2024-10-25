public class Interpreter implements Expr.Visitor<Object> {
    public void interpret(Expr expr){
        try {
            Object value = evaluate(expr);
            System.out.println(stringify(value));
        } catch (RuntimeError error){
            Main.runtimeError(error);
        }
    }

    private String stringify(Object value) {
        if(value == null) return "nil";

        if(value instanceof Double){
            String text = value.toString();
            if(text.endsWith(".0")){
                text = text.substring(0, text.length()-2);
            }
            return text;
        }
        return value.toString();
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = expr.left;
        Object right = expr.right;

        switch (expr.operator.type){
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER: checkNumberOperands(expr.operator, left,right);
                return (double)left > (double)right;
            case GREATER_EQUAL: checkNumberOperands(expr.operator, left,right);
                return (double)left >= (double)right;
            case LESS: checkNumberOperands(expr.operator, left,right);
                return (double)left < (double)right;
            case LESS_EQUAL: checkNumberOperands(expr.operator, left,right);
                return (double)left <= (double)right;
            case MINUS: checkNumberOperands(expr.operator, left,right);
                return (double)left - (double)right;
            case PLUS:
                if(left instanceof Double && right instanceof Double){
                    return (double)left + (double)right;
                } else if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError("Operands must be two numbers or two strings.", expr.operator);
            case SLASH: checkNumberOperands(expr.operator, left,right);
                return (double)left / (double)right;
            case STAR: checkNumberOperands(expr.operator, left,right);
                return (double)left * (double)right;
        }
        return null;
    }



    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type){
            case BANG: return !isTruthy(right);
            case MINUS: checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        return null;
    }

    private boolean isTruthy(Object object) {
        if(object == null) return false;
        if(object instanceof Boolean) return (boolean)object;
        return true;
    }

    private Object evaluate(Expr expr){
        return expr.accept(this);
    }
    private boolean isEqual(Object a, Object b ){
        if(a == null & b == null) return true;
        if(a == null ^ b == null) return false;

        return a.equals(b);
    }
    private void checkNumberOperand(Token operator, Object operand) {
        if(operand instanceof Double) return;
        throw new RuntimeError("Operand must be a number.", operator);
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError("Operands must be numbers.", operator);
    }
}
