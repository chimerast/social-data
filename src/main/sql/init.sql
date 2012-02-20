CREATE DATABASE speeda DEFAULT CHARACTER SET utf8;

USE speeda;

CREATE TABLE company (
  company_id CHAR(12) NOT NULL,
  industry_id CHAR(12),
  country_id CHAR(3) NOT NULL,
  symbol_id VARCHAR(15),
  name VARCHAR(128)NOT NULL,
  url VARCHAR(128),
  address VARCHAR(128),
  description VARCHAR(512),
  period INTEGER,
  employee INTEGER,
  salary DOUBLE,
  age DOUBLE,
  bank VARCHAR(256),
  financial_currency CHAR(3),
  share_currency CHAR(3),
  PRIMARY KEY(company_id)
);

CREATE TABLE share (
  company_id CHAR(12) NOT NULL,
  date DATE NOT NULL,
  price DOUBLE,
  `change` DOUBLE,
  volume DOUBLE,
  market_capitalization DOUBLE,
  PRIMARY KEY(company_id, date)
);

CREATE INDEX company_country_id_idx ON company(country_id);

CREATE TABLE finance (
  company_id CHAR(12) NOT NULL,
  year INTEGER NOT NULL,
  title INTEGER NOT NULL,
  value DOUBLE,
  PRIMARY KEY(company_id, year, title)
);
