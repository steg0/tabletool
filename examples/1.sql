select &param1,&param2,&param1 from sysibm.sysdummy1;

--CSV Result "The only row fetched from derby at Sun Mar 17 12:54:26 CET 2024
--"
--1,2,3
--a,b,a

select 1
from
sysibm.
sysdummy1
;

select 2
from
sysibm.
sysdummy1
;

select 3
from
sysibm.
sysdummy1
;

select 4
from
sysibm.
sysdummy1
;

--CSV Result
--1
--3

select 10
from
sysibm.
sysdummy1
;


select * from sysibm.sysdummy1;
select * from syscat.tables;

select 1,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 2,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 3,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 4,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 5,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 6,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 7,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 8,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 9,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 10,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 11,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 12,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 13,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 14,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 15,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 16,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 17,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 18,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 19,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 20,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 21,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 22,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 23,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
union all select 24,1,2,3,4,5,6,7,8,9,0 from sysibm.sysdummy1
--CSV Result
--1
--10






select * from sysibm.sysdummy1;
--CSV Result
--IBMREQD
--Y


create table tabtype.t(c clob);
insert into tabtype.t(c) values('abc
def');
select * from tabtype.t;
--CSV Result
--C
--org.apache.derby.impl.jdbc.EmbedClob@2851f874

