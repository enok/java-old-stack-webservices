-- ===========================================================================
-- CampusConnect SIS integration schema (MySQL 5.7)
--
-- Loaded by the mysql:5.7 container in docker-compose.yml via
-- /docker-entrypoint-initdb.d.
--
-- XXX: MyISAM on three tables, InnoDB on the rest. Nobody remembers why.
--      The MyISAM tables are the reason EnrollmentDao has no transaction.
-- ===========================================================================

CREATE DATABASE IF NOT EXISTS campusconnect
  DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE campusconnect;

-- ---------------------------------------------------------------- student --
CREATE TABLE IF NOT EXISTS student (
  student_id        VARCHAR(16)  NOT NULL,
  customer_code     VARCHAR(16)  NOT NULL,
  last_name         VARCHAR(64)  NOT NULL,
  first_name        VARCHAR(64)  NOT NULL,
  date_of_birth     DATE         NULL,
  program_code      VARCHAR(16)  NULL,
  enrollment_status VARCHAR(4)   NULL,
  campus_code       VARCHAR(16)  NULL,
  PRIMARY KEY (customer_code, student_id),
  KEY ix_student_lastname (last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------------------------------- riverton extension --
-- SMELL #1: an institution-specific table. Only RIVERTON rows exist here and
-- StudentDao LEFT JOINs it only when customerCode is RIVERTON.
CREATE TABLE IF NOT EXISTS riverton_student_ext (
  student_id           VARCHAR(16) NOT NULL,
  riverton_campus_id   VARCHAR(32) NULL,
  advisor_network_id   VARCHAR(32) NULL,
  residency_indicator  VARCHAR(4)  NULL,
  legacy_banner_pidm   VARCHAR(16) NULL,
  PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ------------------------------------------------------- summit extension --
CREATE TABLE IF NOT EXISTS summit_student_ext (
  student_id   VARCHAR(16) NOT NULL,
  catalog_year VARCHAR(9)  NULL,
  PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ------------------------------------------------------------- enrollment --
-- XXX: MyISAM. No foreign keys, no transactions. See EnrollmentDao.
CREATE TABLE IF NOT EXISTS enrollment (
  id            INT AUTO_INCREMENT,
  customer_code VARCHAR(16) NOT NULL,
  student_id    VARCHAR(16) NOT NULL,
  term_code     VARCHAR(8)  NOT NULL,
  crn           VARCHAR(8)  NOT NULL,
  credit_hours  VARCHAR(4)  NULL,
  status        VARCHAR(2)  NULL,
  PRIMARY KEY (id),
  KEY ix_enrollment_student (customer_code, student_id, term_code)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------------------------------------- advising_hold --
CREATE TABLE IF NOT EXISTS advising_hold (
  id            INT AUTO_INCREMENT,
  customer_code VARCHAR(16) NOT NULL,
  student_id    VARCHAR(16) NOT NULL,
  hold_code     VARCHAR(8)  NOT NULL,
  hold_reason   VARCHAR(255) NULL,
  placed_on     DATE        NULL,
  released_on   DATE        NULL,
  releasable    TINYINT(1)  DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_hold_student (customer_code, student_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ------------------------------------------------------- integration_audit --
-- TODO: written by nothing. The audit requirement was dropped in 2015 but the
-- table survived two migrations.
CREATE TABLE IF NOT EXISTS integration_audit (
  id            INT AUTO_INCREMENT,
  customer_code VARCHAR(16) NULL,
  operation     VARCHAR(64) NULL,
  payload       TEXT        NULL,
  created_on    DATETIME    NULL,
  PRIMARY KEY (id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
