package com.intuit.karate;

import com.intuit.karate.cucumber.FeatureWrapper;
import java.util.Properties;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class ScriptBridge {
            
    private static final Logger logger = LoggerFactory.getLogger(ScriptBridge.class);    
    
    private final ScriptContext context;
    
    public ScriptBridge(ScriptContext context) {
        this.context = context;       
    }

    public ScriptContext getContext() {
        return context;
    }        
    
    public Object read(String fileName) {
        ScriptValue sv = FileUtils.readFile(fileName, context);
        return sv.getValue();
    }
    
    public void set(String name, Object o) {
        context.vars.put(name, o);
    }
    
    public Object get(String exp) {
        ScriptValue sv;
        try {
            sv = Script.eval(exp, context); // even json path expressions will work
        } catch (Exception e) {
            logger.warn("karate.get failed for expression: '{}': {}", exp, e.getMessage());
            return null;
        }
        if (sv != null) {
            return sv.getAfterConvertingToMapIfNeeded();
        } else {
            logger.trace("variable is null or does not exist: {}", exp);
            return null;
        }
    }
    
    public Object call(String fileName) {
        return call(fileName, null);
    }

    public Object call(String fileName, Object arg) {
        ScriptValue sv = FileUtils.readFile(fileName, context);
        switch(sv.getType()) {
            case FEATURE_WRAPPER:
                FeatureWrapper feature = sv.getValue(FeatureWrapper.class);
                return Script.evalFeatureCall(feature, arg, context).getValue();
            case JS_FUNCTION:
                ScriptObjectMirror som = sv.getValue(ScriptObjectMirror.class);
                return Script.evalFunctionCall(som, arg, context).getValue();
            default:
                logger.warn("not a js function or feature file: {} - {}", fileName, sv);
                return null;
        }        
    }
    
    public String getEnv() {
        return context.env.env;
    }
    
    public Properties getProperties() {
        return System.getProperties();
    }
    
    public void log(Object ... objects) {
        logger.info("{}", new LogWrapper(objects));
    }        
    
    // make sure toString() is lazy
    static class LogWrapper {
        
        private final Object[] objects;
        
        LogWrapper(Object ... objects) {
            this.objects = objects;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Object o : objects) {
                sb.append(o).append(' ');                
            }
            return sb.toString();
        }
                
    }
    
}
