# Raw SOAP fixtures

These are real-shaped request/response pairs for each institution, captured at
the wire, one directory per customer.

**They are the seed corpus for step 1 of `docs/MODERNIZATION-BACKLOG.md`.**

Nothing in the build reads them today. That is the point: there is currently no
characterization coverage of the SOAP contract at all, and these files are the
raw material for building it. The first task of the modernization is to turn
each pair into an executable test that posts the request to a running instance
and asserts the response is byte-equivalent (modulo the known-volatile fields
listed below). Only once that harness is green does anything else on the
backlog become safe.

## Known-volatile fields

A byte-for-byte assertion will fail on these. They must be normalised by the
harness, not ignored:

| Field | Operation | Why it moves |
|---|---|---|
| `generatedOn` | `getSummitDegreeAudit` | wall clock, `MM-dd-yyyy` |
| `confirmationId` | `submitEnrollmentChange` | contains the applied-row count |
| element order inside `summitHoldSummaryXml` | `getAdvisingHolds` | XSLT output, escaped into a string |

## What to notice in these files

- `getStudent` for an unknown student returns **HTTP 200 and an empty body
  element**, not a fault (`northlake/getStudent-notfound-response.xml`). The
  same condition on `getAdvisingHolds` returns a declared fault
  (`riverton/getAdvisingHolds-notfound-fault.xml`). That inconsistency is
  smell 5a and it is load-bearing for all three consumers.
- `summit/getAdvisingHolds-response.xml` carries XML **escaped inside an
  `xs:string`**. Any reformatting of that string is a breaking change.
- The `<cc:CampusConnectAuth>` header is present on every request and is
  **not declared in the WSDL**. The secrets below are placeholders.
- `riverton/getStudent-response.xml` uses `rivertonStudentDetail` with four
  extra elements and `dd/MM/yyyy` dates; `northlake/getStudent-response.xml`
  uses `studentDetail` with `yyyy-MM-dd`.

No real student data appears here. All identifiers match `db/seed.sql`.
