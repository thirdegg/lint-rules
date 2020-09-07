![GitHub](https://img.shields.io/github/license/thirdegg/lint-rules.svg)
[![JitPack](https://jitpack.io/v/thirdegg/lint-rules.svg)](https://jitpack.io/#thirdegg/lint-rules)
# Android lint rules

Add to your project ```build.gradle```:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

and to module ```build.gradle```:

```gradle
dependencies {
    ...
    lintChecks('com.github.thirdegg:lint-rules:0.0.6-beta')
}
```

How to update:

Sometimes, after changing the version of a plugin in dependencies, android studio does not give any effect. In order for the changes to work, you need to do `Build > Clear Project` and rebuild the project.

## Checked Exceptions for kotlin
How it works:

![](checked-exceptions.png)
