# xjar-springboot3-maven-plugin

基于 `xjar-springboot3` 的 Maven 插件封装，适配 **JDK 21**，用于在 Maven 构建阶段对普通 JAR 或 Spring Boot JAR 进行加密。

## 环境要求

- JDK 21+
- Maven 3.9+

## 插件坐标

```xml
<plugin>
    <groupId>io.github.cnscottluo</groupId>
    <artifactId>xjar-springboot3-maven-plugin</artifactId>
    <version>0.0.1</version>
</plugin>
```

## 使用方式

建议先在项目中增加 JitPack 仓库（用于解析 `xjar-springboot3` 依赖）：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```


```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.cnscottluo</groupId>
            <artifactId>xjar-springboot3-maven-plugin</artifactId>
            <version>0.0.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>build</goal>
                    </goals>
                    <phase>package</phase>
                    <configuration>
                        <password>${xjar.password}</password>
                        <!-- 可选参数
                        <algorithm>AES/CBC/PKCS5Padding</algorithm>
                        <keySize>128</keySize>
                        <ivSize>128</ivSize>
                        <includes>
                            <include>BOOT-INF/classes/**</include>
                        </includes>
                        <excludes>
                            <exclude>BOOT-INF/classes/static/**</exclude>
                        </excludes>
                        <sourceDir>${project.build.directory}</sourceDir>
                        <sourceJar>${project.build.finalName}.jar</sourceJar>
                        <targetDir>${project.build.directory}</targetDir>
                        <targetJar>${project.build.finalName}.xjar</targetJar>
                        <deletes>
                            <delete>target/*.jar</delete>
                        </deletes>
                        -->
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

或直接命令执行：

```bash
mvn xjar-springboot3:build -Dxjar.******
```

## 参数说明

| 参数 | 命令参数 | 说明 | 默认值 |
| --- | --- | --- | --- |
| password | `-Dxjar.password` | 加密密码（必填） | 无 |
| algorithm | `-Dxjar.algorithm` | 加密算法 | `AES/CBC/PKCS5Padding` |
| keySize | `-Dxjar.keySize` | 密钥长度 | `128` |
| ivSize | `-Dxjar.ivSize` | 向量长度 | `128` |
| sourceDir | `-Dxjar.sourceDir` | 源 JAR 目录 | `${project.build.directory}` |
| sourceJar | `-Dxjar.sourceJar` | 源 JAR 文件名 | `${project.build.finalName}.jar` |
| targetDir | `-Dxjar.targetDir` | 目标 JAR 目录 | `${project.build.directory}` |
| targetJar | `-Dxjar.targetJar` | 目标 xjar 文件名 | `${project.build.finalName}.xjar` |
| includes | `-Dxjar.includes` | 需要加密的资源（Ant 表达式） | 无 |
| excludes | `-Dxjar.excludes` | 需要排除的资源（Ant 表达式） | 无 |
| deletes | `-Dxjar.deletes` | 加密后删除的资源（glob 模式，支持 `../`） | 无 |

## 注意事项

- Spring Boot 工程检测到 `spring-boot-maven-plugin` 时会自动走 `XBoot.encrypt`。
- 目前不支持 `spring-boot-maven-plugin` 配置中的：
    - `<executable>true</executable>`
    - `<embeddedLaunchScript>...</embeddedLaunchScript>`
- 强烈建议不要把密码明文写在 `pom.xml`，优先使用命令行 `-Dxjar.******` 传参。
