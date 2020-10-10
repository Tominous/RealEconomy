CREATE TABLE IF NOT EXISTS trade_logs (
	id INTEGER NOT NULL AUTO_INCREMENT,
	listing_uuid CHAR(36) NOT NULL,
	timestamp DATETIME NOT NULL,
	seller CHAR(36) NOT NULL,
	buyer CHAR(36) NOT NULL,

	price DOUBLE PRECISION NOT NULL,
	currencyUuid CHAR(36) NOT NULL,
	amount INTEGER NOT NULL,

	PRIMARY KEY(id)
);