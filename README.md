StudentRecordService
====================

SOAP services for student record integration.

Build:

    mvn clean install

Deploy studentrecord-webapp/target/studentrecord.war to Tomcat.

WSDL is published at:

    /studentrecord/services/StudentRecordService?wsdl

The contract in src/main/resources/wsdl is the source of truth. Do not edit the
generated classes under target/.

Auth header is required on every call. Credentials come from the properties
file, ask the integration team for the values for each institution.

Batch roster export runs nightly. Output goes to the export directory
configured in the properties.

TODO: the Riverton response has extra elements, document why.
XXX: getStudent returns empty when not found instead of a fault. Known.

---
This repository is a synthetic codebase created as the starting point of a
software modernization exercise. It is not a real product, it has never served a
real user, and it contains no real data or credentials.
