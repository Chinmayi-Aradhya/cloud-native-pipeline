import time
import os
import logging
from kafka import KafkaConsumer
import psycopg2

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP = os.environ.get('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092')
KAFKA_TOPIC = os.environ.get('KAFKA_TOPIC', 'raw_messages')

consumer = KafkaConsumer(
    KAFKA_TOPIC,
    bootstrap_servers=KAFKA_BOOTSTRAP,
    auto_offset_reset='earliest',
    enable_auto_commit=True,
    group_id='python-consumer-group',
    api_version=(3, 3, 0),
)

DB_HOST = os.environ.get('DB_HOST', 'postgres')
DB_NAME = os.environ.get('DB_NAME', 'postgres')
DB_USER = os.environ.get('DB_USER', 'postgres')
DB_PASS = os.environ.get('DB_PASS', 'postgres')

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
    logger.info("Ensured 'messages' table exists in database")

def main():
    conn = None
    while conn is None:
        try:
            conn = psycopg2.connect(
                host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASS
            )
            create_table_if_not_exists(conn)
            logger.info("Successfully connected to PostgreSQL and prepared table")
        except psycopg2.OperationalError as e:
            logger.warning("PostgreSQL not ready yet - retrying in 5 seconds (%s)", e)
            time.sleep(5)
        except Exception as e:
            logger.error("Failed to connect to PostgreSQL", exc_info=True)
            time.sleep(10)

    logger.info("Starting Kafka consumer loop for topic 'raw_messages'")

    try:
        for message in consumer:
            try:
                raw = message.value.decode('utf-8')
                transformed = raw.upper()

                with conn:
                    with conn.cursor() as cur:
                        cur.execute(
                            "INSERT INTO messages (content) VALUES (%s)",
                            (transformed,)
                        )

                logger.info("Inserted message: '%s' (offset: %d, partition: %d)",
                            transformed, message.offset, message.partition)

            except UnicodeDecodeError:
                logger.error("Failed to decode message at offset %d - invalid UTF-8", message.offset)
            except psycopg2.Error as e:
                logger.error("Database insert failed for message '%s' (offset %d): %s",
                             raw, message.offset, e.pgerror or str(e))
            except Exception as e:
                logger.error("Unexpected error while processing message (offset %d)", message.offset, exc_info=True)

    except Exception as e:
        logger.critical("Kafka consumer loop terminated unexpectedly", exc_info=True)
    finally:
        logger.info("Shutting down consumer")
        if consumer:
            consumer.close()
        if conn:
            conn.close()

if __name__ == "__main__":
    main()