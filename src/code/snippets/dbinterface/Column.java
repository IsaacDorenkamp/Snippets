package code.snippets.dbinterface;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
  String name() default "";
  boolean generated() default false; // true for fields which are automatically generated, like auto-increment fields
}
