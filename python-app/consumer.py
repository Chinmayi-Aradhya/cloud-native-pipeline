import time
from kafka import KafkaConsumer
import psycopg2
from psycopg2.extras import execute_values

consumer = KafkaConsumer(
    'raw_messages',
    bootstrap_servers='kafka:9092',
    auto_offset_reset='earliest',
    enable_auto_commit=True,
    group_id='python-consumer-group'
)

DB_HOST = 'postgres'
DB_NAME = 'postgres'
DB_USER = 'postgres'
DB_PASS = 'postgres'  

def create_table_if_not_exists(conn):
    with conn.cursor() as cur:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                content TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
    conn.commit()

def main():
    conn = None
    while conn is None:
        try:
            conn = psycopg2.connect(
                host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASS
            )
            create_table_if_not_exists(conn)
            print("Connected to Postgres and table ready.")
        except psycopg2.OperationalError:
            print("Waiting for Postgres...")
            time.sleep(5)

    for message in consumer:
        raw = message.value.decode('utf-8')
        transformed = raw.upper()
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO messages (content) VALUES (%s)",
                (transformed,)
            )
        conn.commit()
        print(f"Inserted: {transformed}")

if __name__ == "__main__":
    main()
