-- ===========================================================================
-- Seed data for local development and for the SOAP fixtures in
-- studentrecord-service/src/test/resources/soap/.
--
-- All names, ids and identifiers below are invented. There is no real student
-- data anywhere in this repository.
-- ===========================================================================

USE campusconnect;

INSERT IGNORE INTO student
  (student_id, customer_code, last_name, first_name, date_of_birth, program_code, enrollment_status, campus_code)
VALUES
  ('N0010001', 'NORTHLAKE', 'Abernathy', 'Dana',   '1999-04-12', 'BSCS', 'A', 'NL-MAIN'),
  ('N0010002', 'NORTHLAKE', 'Okafor',    'Tunde',  '2000-11-02', 'BSEE', 'A', 'NL-MAIN'),
  ('N0010003', 'NORTHLAKE', 'Vasquez',   'Mireia', '2001-01-30', 'BA',   'P', 'NL-WEST'),
  ('R0200455', 'RIVERTON',  'Lindqvist', 'Sanna',  '1998-07-19', 'BBA',  'A', 'RV-CENTRAL'),
  ('R0200456', 'RIVERTON',  'Adeyemi',   'Kolade', '2002-03-08', 'BSN',  'A', 'RV-NORTH'),
  ('R0200457', 'RIVERTON',  'Petrov',    'Ilya',   '1997-12-25', 'BBA',  'W', 'RV-CENTRAL'),
  ('S9900771', 'SUMMIT',    'Chaudhary', 'Nikhil', '2000-06-05', 'BSME', 'A', 'SM-HILL'),
  ('S9900772', 'SUMMIT',    'Brannigan', 'Orla',   '1999-09-17', 'BSCS', 'A', 'SM-HILL');

INSERT IGNORE INTO riverton_student_ext
  (student_id, riverton_campus_id, advisor_network_id, residency_indicator, legacy_banner_pidm)
VALUES
  ('R0200455', 'RVC-88120', 'slindqvist', 'IS', '000145522'),
  ('R0200456', 'RVC-88121', 'kadeyemi',   'OS', '000145523'),
  ('R0200457', 'RVC-88122', 'ipetrov',    'IS', '000145524');

INSERT IGNORE INTO summit_student_ext (student_id, catalog_year) VALUES
  ('S9900771', '2019-2020'),
  ('S9900772', '2018-2019');

INSERT IGNORE INTO enrollment (customer_code, student_id, term_code, crn, credit_hours, status) VALUES
  ('NORTHLAKE', 'N0010001', '202410', '10221', '3', 'A'),
  ('NORTHLAKE', 'N0010001', '202410', '10344', '4', 'A'),
  ('NORTHLAKE', 'N0010002', '202410', '10221', '3', 'A'),
  ('NORTHLAKE', 'N0010003', '202410', '11002', '3', 'A'),
  ('RIVERTON',  'R0200455', '202410', '20455', '3', 'A'),
  ('RIVERTON',  'R0200456', '202410', '20455', '3', 'A'),
  ('RIVERTON',  'R0200457', '202410', '20901', '3', 'A'),
  ('SUMMIT',    'S9900771', '202410', '30770', '4', 'A'),
  ('SUMMIT',    'S9900772', '202410', '30770', '4', 'A');

INSERT IGNORE INTO advising_hold
  (customer_code, student_id, hold_code, hold_reason, placed_on, released_on, releasable)
VALUES
  ('NORTHLAKE', 'N0010003', 'ADV', 'Advising appointment required before registration', '2024-01-08', NULL, 1),
  ('NORTHLAKE', 'N0010001', 'LIB', 'Library materials outstanding', '2023-09-01', '2023-10-01', 1),
  ('RIVERTON',  'R0200457', 'FIN', 'Outstanding balance', '2023-11-14', NULL, 0),
  ('RIVERTON',  'R0200457', 'ADV', 'Programme change review', '2024-01-02', NULL, 1),
  ('SUMMIT',    'S9900771', 'IMM', 'Immunisation record incomplete', '2023-08-22', NULL, 0),
  ('SUMMIT',    'S9900771', 'ADV', 'Degree audit review', '2024-01-11', NULL, 1);

-- XXX: CustomerCodes.LAKESHORE_RETIRED still has rows. Two migrations have
-- failed to remove them because the delete script times out on MyISAM.
INSERT IGNORE INTO student
  (student_id, customer_code, last_name, first_name, date_of_birth, program_code, enrollment_status, campus_code)
VALUES
  ('L0000001', 'LAKESHORE', 'Doyle', 'Fintan', '1994-02-02', 'BA', 'X', 'LS-OLD');
