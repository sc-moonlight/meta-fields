# Meta-Fields

An annotation processor, inspired by [JPA Static Metamodel](https://docs.jboss.org/hibernate/orm/5.4/topical/html_single/metamodelgen/MetamodelGenerator.html)

## Overview

[Java][java] provides Method references, but if you need to access a property of a DTO by name, you still need to reference it by using a String.

This leads to code that is prone to mistakes.

My original motivation was from generating a meta-model for editing a DTO with a React frontend.  I needed to create a map of properties to form data, that could be mapped back to a data DTO when the form was saved.  It was much too easy to misspell a field.




A more common example is using rejectValue() from Spring's [Errors](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/Errors.html) interface. This allows you to reject the value for a specific field in your DTO.
```java
errors.rejectValue("userId", "invalid");
```

However, there is no guarantee that "userId" is a property in the DTO. Which will result in a runtime problem.
If you rename userId to userName, it is easy to miss updating anywhere that "userId" was used. 

You could create constants for all your DTO properties, but those must then be manually managed.

This processor adds an easy to use Meta DTO class with references to the original DTO properties.

## Example usage
Let's start with a simple User DTO.
```java
@GenStaticMeta
public class UserDTO {
    private String id;
    private String firstName;
    private String lastName;
    private Integer customerId;
}
```
This will automatically create this class
```java
@Generated(value = "moonlight.annotationproc.staticmeta.GenStaticModelProcessor", date = "2023-11-05T10:15:30.346441400-05:00")
@StaticModel(UserDTO.class)
public final class UserDTOMeta {
  private UserDTOMeta() {}

  public static final String id = "id";
  public static final String firstName = "firstName";
  public static final String lastName = "lastName";
  public static final String customerId = "customerId";
  public static final List<String> allFields = List.of(id,firstName,lastName,customerId);
}
```

The original example now would change to this: 
```java
errors.rejectValue(UserDTOMeta.id, "invalid");
```




## License

    Copyright 2023 Scott Carlson

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
