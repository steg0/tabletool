@start %JAVA_HOME%\bin\javaw %JAVA_OPTS% -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=runtime.logging.properties ^
    -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel ^
    -p ^
%USERPROFILE%\.m2\repository\org\apache\derby\derbyshared\10.15.2.0\derbyshared-10.15.2.0.jar;^
%USERPROFILE%\.m2\repository\org\apache\derby\derby\10.15.2.0\derby-10.15.2.0.jar;^
%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.3.1\postgresql-42.3.1.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\jdbc\ojdbc11\21.7.0.0\ojdbc11-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\xml\xdb\21.7.0.0\xdb-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\xml\xmlparserv2\21.7.0.0\xmlparserv2-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\security\osdt_core\21.7.0.0\osdt_core-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\security\osdt_cert\21.7.0.0\osdt_cert-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\oracle\database\security\oraclepki\21.7.0.0\oraclepki-21.7.0.0.jar;^
%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.5.7.0\jcc-11.5.7.0.jar ^
    -jar target/tabtype.jar ^
    -config test.tabtype.properties ^
    -- ^
    %1 %2 %3 %4 %5 %6 %7 %8 %9
