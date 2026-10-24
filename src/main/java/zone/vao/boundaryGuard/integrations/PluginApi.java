package zone.vao.boundaryGuard.integrations;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Access to optional APIs whose artifacts are not published in a public Maven repository.
 * Resolution uses the protection plugin's class loader; resolved methods are cached per adapter.
 */
final class PluginApi {
  private final ClassLoader loader;
  private final Map<Key, Method> methods = new ConcurrentHashMap<>();

  PluginApi(Plugin plugin) { loader = plugin.getClass().getClassLoader(); }

  Class<?> type(String name) {
    try { return Class.forName(name, true, loader); }
    catch (ClassNotFoundException failure) { throw new IllegalStateException("Missing plugin API: " + name, failure); }
  }

  Object constant(String type, String name) {
    try { return type(type).getField(name).get(null); }
    catch (ReflectiveOperationException failure) { throw new IllegalStateException("Missing API constant: " + type + "." + name, failure); }
  }

  Object create(String type, Class<?> parameterType, Object parameter) {
    try { return type(type).getConstructor(parameterType).newInstance(parameter); }
    catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot initialize API: " + type, failure); }
  }

  Object call(Object target, String name, Object... arguments) {
    Class<?> type = target instanceof Class<?> c ? c : target.getClass();
    boolean isStatic = target instanceof Class<?>;
    Key key = new Key(type, name, isStatic, Arrays.stream(arguments).map(Object::getClass).toList());
    Method method = methods.computeIfAbsent(key, ignored -> resolve(key, arguments));
    try { return method.invoke(isStatic ? null : target, arguments); }
    catch (IllegalAccessException failure) { throw new IllegalStateException("Cannot access API: " + method, failure); }
    catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new IllegalStateException("Plugin API failed: " + method, cause);
    }
  }

  private Method resolve(Key key, Object[] arguments) {
    Method found = null;
    for (Method method : key.type().getMethods()) {
      if (!method.getName().equals(key.name()) || method.getParameterCount() != arguments.length
          || Modifier.isStatic(method.getModifiers()) != key.isStatic()) continue;
      Class<?>[] parameters = method.getParameterTypes();
      boolean matches = true;
      for (int i = 0; i < parameters.length; i++) {
        Class<?> parameter = parameters[i] == boolean.class ? Boolean.class : parameters[i];
        if (!parameter.isInstance(arguments[i])) { matches = false; break; }
      }
      if (!matches) continue;
      if (found == null || moreSpecific(method, found)) found = method;
      else if (!moreSpecific(found, method) && !Arrays.equals(found.getParameterTypes(), parameters)) {
        throw new IllegalStateException("Ambiguous API method: " + key.type().getName() + "." + key.name());
      }
    }
    if (found == null) throw new IllegalStateException("Unsupported plugin API: " + key.type().getName() + "." + key.name());
    return found;
  }

  private boolean moreSpecific(Method candidate, Method other) {
    Class<?>[] a = candidate.getParameterTypes();
    Class<?>[] b = other.getParameterTypes();
    for (int i = 0; i < a.length; i++) if (!b[i].isAssignableFrom(a[i])) return false;
    return true;
  }

  private record Key(Class<?> type, String name, boolean isStatic, List<Class<?>> arguments) {}
}
