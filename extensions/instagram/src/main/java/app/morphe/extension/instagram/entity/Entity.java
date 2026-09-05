/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Constructor;

public class Entity {
    protected final Object obj;

    public Entity(Object obj) {
        this.obj = obj;
    }

    public Entity() {
        this.obj = null;
    }

    public Object getObject(){
        return this.obj;
    }

    public Class<?> getObjClass() throws ClassNotFoundException {
        return this.obj.getClass();
    }

    public Entity construct(String className, Class<?>[] paramTypes, Object... params) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(params);
        return new Entity(instance);
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) throws NoSuchMethodException {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ignored) {
        }
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        // Polymorphic matching fallback (interfaces, subclasses, primitives)
        current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && isParametersCompatible(m.getParameterTypes(), paramTypes)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            current = current.getSuperclass();
        }

        throw new NoSuchMethodException("Method " + methodName + " not found in " + clazz);
    }

    private static boolean isParametersCompatible(Class<?>[] declaredTypes, Class<?>[] actualTypes) {
        if (declaredTypes.length != actualTypes.length) {
            return false;
        }
        for (int i = 0; i < declaredTypes.length; i++) {
            Class<?> dec = declaredTypes[i];
            Class<?> act = actualTypes[i];
            if (act == null) {
                if (dec.isPrimitive()) return false;
                continue;
            }
            if (dec.isAssignableFrom(act) || isWrapperMatch(dec, act)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isWrapperMatch(Class<?> dec, Class<?> act) {
        if (dec == int.class && act == Integer.class) return true;
        if (dec == long.class && act == Long.class) return true;
        if (dec == boolean.class && act == Boolean.class) return true;
        if (dec == byte.class && act == Byte.class) return true;
        if (dec == char.class && act == Character.class) return true;
        if (dec == short.class && act == Short.class) return true;
        if (dec == float.class && act == Float.class) return true;
        if (dec == double.class && act == Double.class) return true;
        if (act == int.class && dec == Integer.class) return true;
        if (act == long.class && dec == Long.class) return true;
        if (act == boolean.class && dec == Boolean.class) return true;
        return false;
    }

    public Object getField(Class cls, Object clsObj, String fieldName) throws Exception {
        Class<?> current = cls;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(clsObj);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in " + cls);
    }

    public Object getField(Object clsObj, String fieldName) throws Exception {
        if (clsObj == null) return null;
        Class<?> cls = (clsObj instanceof Class<?>) ? (Class<?>) clsObj : clsObj.getClass();
        return getField(cls, (clsObj instanceof Class<?>) ? null : clsObj, fieldName);
    }

    public Object getField(String fieldName) throws Exception {
        return getField(this.obj, fieldName);
    }

    public Entity getFieldAsEntity(String fieldName) throws Exception {
        Object object = getField(fieldName);
        return object == null ? null : new Entity(object);
    }

    public Object getMethod(Object clsObj, String methodName, Class<?>[] paramTypes, Object... params) throws Exception {
        if (clsObj == null) {
            return null;
        }
        Class<?> clazz;
        Object targetInstance;
        if (clsObj instanceof Class<?>) {
            clazz = (Class<?>) clsObj;
            targetInstance = null;
        } else {
            clazz = clsObj.getClass();
            targetInstance = clsObj;
        }

        Method method = findMethod(clazz, methodName, paramTypes);
        method.setAccessible(true);

        return method.invoke(targetInstance, params);
    }

    public Object getMethod(Object clsObj, String methodName, Object... params) throws Exception {
        if (clsObj == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            Class<?> clazz = (clsObj instanceof Class<?>) ? (Class<?>) clsObj : clsObj.getClass();
            Object targetInstance = (clsObj instanceof Class<?>) ? null : clsObj;
            Method method = findMethod(clazz, methodName, new Class<?>[0]);
            method.setAccessible(true);
            return method.invoke(targetInstance);
        } else {
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramTypes[i] = params[i] == null ? Object.class : params[i].getClass();
            }
            return this.getMethod(clsObj, methodName, paramTypes, params);
        }

    }

    public Object getMethod(String methodName, Class<?>[] paramTypes, Object... params) throws Exception {
        return this.getMethod(this.obj, methodName, paramTypes, params);
    }

    public Object getMethod(String methodName, Object... params) throws Exception {
        return this.getMethod(this.obj, methodName, params);
    }

}
