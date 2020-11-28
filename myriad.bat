%JAVA_HOME%\bin\java -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -p %USERPROFILE%\.m2\repository\com\oracle\ojdbc7\12.1.0.1\ojdbc7-12.1.0.1.jar;target\classes ^
    -m de.steg0.deskapps.tabletool/de.steg0.deskapps.tabletool.Myriad ^
    -config test.myriad.properties ^
    test.myr.xml
