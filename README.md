### How to use
#### Git submodule

1. Add to the project
```cmd
git submodule add https://github.com/flsrg/llm-polling-bot
```
2. Update after clone
```cmd
git submodule init
git submodule update
```
3. Configure gradle
- build.gradle.kts
```kts
dependencies {
    implementation(project(":llm-polling-bot"))
}
```
- settings.gradle.kts
```kts
include(":llm-polling-bot")
project(":llm-polling-bot").projectDir = file("libs/llm-polling-bot") 
```
4. Update submodule to the latest commit
```cmd
git submodule update --remote
```