-- PostgreSQL�����
-- ������h����n\

-- �z(������
CREATE DATABASE readscape_dev;
GRANT ALL PRIVILEGES ON DATABASE readscape_dev TO readscape_user;

-- ƹ�(������  
CREATE DATABASE readscape_test;
GRANT ALL PRIVILEGES ON DATABASE readscape_test TO readscape_user;

-- )Pn-�
ALTER USER readscape_user CREATEDB;