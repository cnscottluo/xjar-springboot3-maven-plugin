# xjar-springboot3-maven-plugin

基于 `xjar-springboot3` 的 Maven 插件封装，适配 **JDK 21**，用于在 Maven 构建阶段对普通 JAR 或 Spring Boot JAR 进行加密。

## 环境要求

- JDK 21+
- Maven 3.9+

## 集成步骤

```xml

<project>
    <!-- 设置 jitpack.io 插件仓库 -->
    <pluginRepositories>
        <pluginRepository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </pluginRepository>
    </pluginRepositories>
    <!-- 添加 XJar Maven 插件 -->
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.cnscottluo</groupId>
                <artifactId>xjar-springboot3-maven-plugin</artifactId>
                <version>3.5.15</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                        <phase>package</phase>
                        <configuration>
                            <password>io.xjar</password>
                            <!-- optional
                            <algorithm/>
                            <keySize/>
                            <ivSize/>
                            <includes>
                                <include/>
                            </includes>
                            <excludes>
                                <exclude/>
                            </excludes>
                            <sourceDir/>
                            <sourceJar/>
                            <targetDir/>
                            <targetJar/>
                            -->
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>

```

#### 也可以通过Maven命令单独执行 XJar 插件

```text
mvn xjar:build -Dxjar.password=io.xjar
mvn xjar:build -Dxjar.password=io.xjar -Dxjar.targetDir=/directory/to/save/target.xjar
```

#### 但通常情况下是让XJar插件绑定到指定的phase中自动执行，这样就能在项目构建的时候自动构建出加密的包。

```text
mvn clean package -Dxjar.password=io.xjar
mvn clean install -Dxjar.password=io.xjar -Dxjar.targetDir=/directory/to/save/target.xjar
```

## 强烈建议

强烈建议不要在 pom.xml 的 xjar-maven-plugin 配置中写上密码，这样会导致打包出来的 xjar 包中的 pom.xml 文件保留着密码，极其容易暴露密码！强烈推荐通过
mvn 命令来指定加密密钥！

## 注意事项

#### 不兼容 spring-boot-maven-plugin 的 executable = true 以及 embeddedLaunchScript

```xml

<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <!-- 需要将executable和embeddedLaunchScript参数删除，目前还不能支持对该模式Jar的加密！后面可能会支持该方式的打包。 
    <configuration>
        <executable>true</executable>
        <embeddedLaunchScript>...</embeddedLaunchScript>
    </configuration>
    -->
</plugin>
```

## 参数说明

| 参数名称      | 命令参数名称           | 参数说明           | 参数类型     | 缺省值                             | 示例值                                                     |
|:----------|:-----------------|:---------------|:---------|:--------------------------------|:--------------------------------------------------------|
| password  | -Dxjar.password  | 密码字符串          | String   | 必须                              | 任意字符串，io.xjar                                           |
| algorithm | -Dxjar.algorithm | 加密算法名称         | String   | AES/CBC/PKCS5Padding            | JDK内置加密算法，如：AES/CBC/PKCS5Padding 和 DES/CBC/PKCS5Padding |
| keySize   | -Dxjar.keySize   | 密钥长度           | int      | 128                             | 根据加密算法而定，56，128，256                                     |
| ivSize    | -Dxjar.ivSize    | 密钥向量长度         | int      | 128                             | 根据加密算法而定，128                                            |
| sourceDir | -Dxjar.sourceDir | 源jar所在目录       | File     | ${project.build.directory}      | 文件目录                                                    |
| sourceJar | -Dxjar.sourceJar | 源jar名称         | String   | ${project.build.finalName}.jar  | 文件名称                                                    |
| targetDir | -Dxjar.targetDir | 目标jar存放目录      | File     | ${project.build.directory}      | 文件目录                                                    |
| targetJar | -Dxjar.targetJar | 目标jar名称        | String   | ${project.build.finalName}.xjar | 文件名称                                                    |
| includes  | -Dxjar.includes  | 需要加密的资源路径表达式   | String[] | 无                               | com/company/project/** , mapper/*Mapper.xml , 支持Ant表达式  |
| excludes  | -Dxjar.excludes  | 无需加密的资源路径表达式   | String[] | 无                               | static/** , META-INF/resources/** , 支持Ant表达式            |
| deletes   | -Dxjar.deletes   | 加密后删除指定资源路径表达式 | String[] | 无                               | target/\*.jar, ../module/target/\*.jar, 支持Ant表达式        |

* 指定加密算法的时候密钥长度以及向量长度必须在算法可支持范围内, 具体加密算法的密钥及向量长度请自行百度或谷歌.
* 当 includes 和 excludes 同时使用时即加密在includes的范围内且排除了excludes的资源。

