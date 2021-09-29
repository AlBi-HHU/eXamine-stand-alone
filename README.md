eXamine
=======
This stand-alone version of eXamine derives from the [Cytoscape app eXamine](https://github.com/ls-cwi/eXamine).
It supports a set-oriented visual analysis approach for annotated network modules.
These network modules are displayed as node-link diagrams with colored contours
on top to depict sets of nodes that share an annotation.

![Screenshot](doc/screenshot.png)

Compile
-------
- Requires (Open) JDK 1.7 or higher: https://openjdk.java.net/install

    Linux:
    ```
    sudo apt-get install default-jdk
    ```

    Mac:
    ```
    brew install openjdk
    ```

- Requires Apache Maven 3.8 or higher: https://maven.apache.org/download.cgi

    Linux:
    ```
    sudo apt-get install maven
    ```

    Mac:
    ```
    brew install maven
    ```

- Get eXamine from GitHub:
    ```
    git clone <HTTPS clone URL (see on the right side of this page)>
    ```

- Compile eXamine:
    ```
    cd eXamine-stand-alone
    mvn install
    ```

Run
---
The above compilation results in a self-executing `eXamine.jar` in the `eXamine-stand-alone` directory. If your OS does not launch eXamine upon opening it directly, try:
```
java -jar eXamine.jar
```

Data
---
Datasets are packaged with eXamine during compilation, from `data-sets` into `eXamine.jar`, and accessed directly when running eXamine.
