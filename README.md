### The Cloud-Native Pipeline

End-to-end event-driven data processing pipeline demonstrating modern cloud-native practices:

- **File** → **Java Producer** → **Apache Kafka** → **Python Consumer** → **PostgreSQL**
- Local development with Docker Compose + observability (AKHQ)
- Production-like deployment on Kubernetes using Helm + GitOps (ArgoCD)

## 🎯 Learning Objectives

- Microservices in different languages communicating via Kafka
- Decoupled, resilient event-driven architecture
- Containerization best practices (multi-stage builds)
- Persistent storage & volume handling in containers
- Helm chart creation & value overrides
- GitOps workflow with ArgoCD

Observability: AKHQ web UI[](http://localhost:8080)

## Steps to run this project
git clone https://github.com/Chinmayi-Aradhya/cloud-native-pipeline
cd cloud-native-pipeline

docker-compose up -d

--> Insert any .txt file inside data/input folder manually or
echo -e "Hello world\nThis is a test line\nFinal message" > data/input/test-001.txt

---> Open the browser and search for 
http://localhost:8080
(you can see the topics and texts whatever you have entered)

---> Open the DBeaver
File -> New -> DBeaver -> Database Connection -> next -> postgresql -> next -> (enter the below data) -> Test Connection -> Finish
host: localhost
database: postgres
port: 5432
username: postgres
password: postgres

--> Afetr the connection is ready and connected succesfully open the SQL editor of the connection and type
select * form messages

### This return the text line by line which has been converted ot Capital letter

