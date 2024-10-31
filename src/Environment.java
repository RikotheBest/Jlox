import java.util.HashMap;
import java.util.Map;
public class Environment {
    final Environment enclosing;

    public Environment() {
        enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value){
        values.put(name, value);
    }
    Object get(Token name){
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }
        if(enclosing !=null) return enclosing.get(name);
        throw new RuntimeError("Undefined variable '" + name.lexeme + "'.", name);
    }
    void assign(Token name, Object value){
        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme,value);
            return;
        }
        if(enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError("Undefined variable '" + name.lexeme + "'.", name);
    }


}

