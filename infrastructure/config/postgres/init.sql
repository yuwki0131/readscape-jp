-- PostgreSQL¹¯ê×È
-- Çü¿Ùü¹hæü¶ün\

-- ‹z(Çü¿Ùü¹
CREATE DATABASE readscape_dev;
GRANT ALL PRIVILEGES ON DATABASE readscape_dev TO readscape_user;

-- Æ¹È(Çü¿Ùü¹  
CREATE DATABASE readscape_test;
GRANT ALL PRIVILEGES ON DATABASE readscape_test TO readscape_user;

-- )Pn-š
ALTER USER readscape_user CREATEDB;