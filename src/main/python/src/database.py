import psycopg2


class Database:
    def __init__(self, dbname, user, password, host, port):
        self.dbname = dbname
        self.user = user
        self.password = password
        self.host = host
        self.port = port

    def __enter__(self):
        self.connection = psycopg2.connect(
            dbname=self.dbname,
            user=self.user,
            password=self.password,
            host=self.host,
            port=self.port
        )
        self.cursor = self.connection.cursor()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.cursor.close()
        self.connection.close()

    def select(self, table, columns='*', where=None):
        query = f"SELECT {columns} FROM {table}"
        if where:
            query += f" WHERE {where}"
        self.cursor.execute(query)
        return self.cursor.fetchall()

    def insert(self, table: str, values: dict):
        keys = f"({', '.join(tuple(values.keys()))})"
        query = f"INSERT INTO {table} {keys} VALUES {tuple(values.values())}"
        self.cursor.execute(query)
        self.connection.commit()

    def update(self, table, set_values, where):
        query = f"UPDATE {table} SET {set_values} WHERE {where}"
        self.cursor.execute(query)
        self.connection.commit()

    def delete(self, table, where):
        query = f"DELETE FROM {table} WHERE {where}"
        self.cursor.execute(query)
        self.connection.commit()


if __name__ == "__main__":
    pass
