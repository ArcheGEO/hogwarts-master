--DROP TABLE IF EXISTS gdsrecord;
CREATE SEQUENCE IF NOT EXISTS seq;
CREATE SEQUENCE IF NOT EXISTS viewseq;
CREATE TABLE IF NOT EXISTS gdsrecordview(gdsid INTEGER NOT NULL, organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), samplenum INTEGER, fname VARCHAR(255), inputorganism VARCHAR(50) NOT NULL, inputdisease VARCHAR(50) NOT NULL, validrecord BOOLEAN, organismmismatch BOOLEAN, diseasemismatch BOOLEAN, diseaseunverified BOOLEAN, PRIMARY KEY(gdsid, inputorganism, inputdisease));
CREATE TABLE IF NOT EXISTS gdsrecord(gdsid INTEGER PRIMARY KEY, organism VARCHAR(255), title VARCHAR(1000), summary VARCHAR(5000), platform VARCHAR(50), samplenum INTEGER, fname VARCHAR(255));
CREATE TABLE IF NOT EXISTS gds_title_mapto_disease(gdsid INTEGER, disease VARCHAR(255), PRIMARY KEY (gdsid, disease));
CREATE TABLE IF NOT EXISTS gds_title_mapto_umls(gdsid INTEGER, umlsid VARCHAR(40), umlsname VARCHAR(255), PRIMARY KEY (gdsid, umlsid));
CREATE TABLE IF NOT EXISTS gds_description_mapto_disease(gdsid INTEGER, disease VARCHAR(255), PRIMARY KEY (gdsid, disease));
CREATE TABLE IF NOT EXISTS gds_description_mapto_umls(gdsid INTEGER, umlsid VARCHAR(40), umlsname VARCHAR(255), PRIMARY KEY (gdsid, umlsid));
CREATE TABLE IF NOT EXISTS gds_titledescription_mapto_disease(gdsid INTEGER, disease VARCHAR(255), PRIMARY KEY (gdsid, disease));
CREATE TABLE IF NOT EXISTS gds_titledescription_mapto_umls(gdsid INTEGER, umlsid VARCHAR(40), umlsname VARCHAR(255), PRIMARY KEY (gdsid, umlsid));
CREATE TABLE IF NOT EXISTS umls_mapto_ncit(umlsid VARCHAR(40), ncitid VARCHAR(40), PRIMARY KEY (umlsid, ncitid));
--INSERT INTO cities(name) VALUES('Bratislava');
--INSERT INTO cities(name) VALUES('Budapest');
--INSERT INTO cities(name) VALUES('Prague');
--INSERT INTO cities(name) VALUES('Warsaw');
--INSERT INTO cities(name) VALUES('Los Angeles');
--INSERT INTO cities(name) VALUES('New York');
--INSERT INTO cities(name) VALUES('Edinburgh');
--INSERT INTO cities(name) VALUES('Berlin');