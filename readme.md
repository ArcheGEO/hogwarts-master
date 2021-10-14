# ArcheGEO (Automated Relevance Checker for GEO) [Online Pipeline]

ArcheGEO is a natural language processing (NLP) resource that improves results from the GEO Browser by automatically determining the relevance of these results. Unlike existing tools, ArcheGEO reports on the irrelevant results and provides reasoning for their exclusion. Such reasoning can be leveraged to improve annotations of GEO results metadata. 

ArcheGEO consists of an online and an offline pipeline. The offline pipeline handles the preprocessing of controlled vocabularies and GDS (GEO dataset) metadata, whereas the online pipeline performs relevance validation. 

This project is the online pipeline of ArcheGEO and is coded using springboot(backend)-vue(frontend). 
It is a maven project created using Visual Studio Code (v1.61.0), PostgreSQL (v9.6) and Java (OpenJDK 11.0.12)
