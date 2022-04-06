# ArcheGEO (Automated Relevance Checker for GEO) [Online Pipeline]

Important: Although PROTEASE is an extension of ArcheGEO, the postgreSQL DB schema of PROTEASE is slightly different from ArcheGEO. Do not run PROTEASE using the postgreSQL DB populated by ArcheGEO_offline and vice versas. Instead, run PROTEASE_offline to populate the postgreSQL DB schema for PROTEASE.
___________________________________________________________________________________________________________________________________

ArcheGEO is a natural language processing (NLP) resource that improves results from the GEO Browser by automatically determining the relevance of these results. Unlike existing tools, ArcheGEO reports on the irrelevant results and provides reasoning for their exclusion. Such reasoning can be leveraged to improve annotations of GEO results metadata. 

ArcheGEO consists of an online and an offline pipeline. The offline pipeline (https://github.com/ArcheGEO/ArcheGEO_offline) handles the preprocessing of controlled vocabularies and GDS (GEO dataset) metadata, whereas the online pipeline performs relevance validation. 

This project is the online pipeline of ArcheGEO and is coded using springboot(backend)-vue(frontend). 
It is a maven project created using Visual Studio Code (v1.61.0), PostgreSQL (v9.6) and Java (OpenJDK 11.0.12)
