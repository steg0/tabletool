-- connect derby
select 'ä',d.* from sysibm.sysdummy1 d;
--CSV Result "The only row fetched from derby at Sat May 04 11:01:40 CEST 2024
--"
--1,IBMREQD
--ä,Y


--CSV Result "The only row fetched from derby in 0 ms at Sun Oct 01 11:37:17 CEST 2023
--"
--IBMREQD
--Y


--CSV Result "The only row fetched from derby in 73 ms at Sun Oct 01 11:37:14 CEST 2023
--"
--IBMREQD
--Y

select tablename
from sys.systables
where tablename=?
;
--CSV Result "The only row fetched from derby at Mon Feb 10 21:16:34 CET 2025
--"
--TABLEID,TABLENAME,TABLETYPE,SCHEMAID,LOCKGRANULARITY
--80000018-00d0-fd77-3ed8-000a0a0b1900,SYSTABLES,S,8000000d-00d0-fd77-3ed8-000a0a0b1900,R

create procedure test1(out varchar(200))
