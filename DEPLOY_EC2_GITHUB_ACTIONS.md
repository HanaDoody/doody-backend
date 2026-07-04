# EC2 + GitHub Actions 백엔드 자동 배포 가이드

작성일: 2026-07-04  
대상: Spring Boot 백엔드 jar 배포

## 1. 배포 구조

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub
    participant GA as GitHub Actions
    participant EC2 as EC2
    participant S3 as S3

    Dev->>GH: git push main
    GH->>GA: workflow 실행
    GA->>GA: Java 21 설정 및 bootJar 빌드
    GA->>EC2: app.jar 업로드
    GA->>EC2: systemctl restart doody-backend
    EC2->>S3: IAM Role로 증빙 이미지 업로드
```

## 2. GitHub Actions Workflow

파일 위치:

```txt
.github/workflows/deploy-ec2.yml
```

동작 조건:

| 조건 | 설명 |
|---|---|
| `main` 브랜치 push | 자동 배포 실행 |
| `workflow_dispatch` | GitHub Actions 화면에서 수동 실행 가능 |

현재 브랜치가 `main`이 아니라면 workflow의 branches 값을 원하는 브랜치로 바꿔야 한다.

예:

```yml
on:
  push:
    branches:
      - refactor/#14
```

## 3. GitHub Secrets 설정

GitHub repository에서 아래 위치로 이동한다.

```txt
Settings -> Secrets and variables -> Actions -> New repository secret
```

아래 값을 추가한다.

| Secret 이름 | 예시 | 설명 |
|---|---|---|
| `EC2_HOST` | `13.124.12.34` | EC2 Public IP 또는 도메인 |
| `EC2_USERNAME` | `ubuntu` | EC2 접속 사용자명 |
| `EC2_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----...` | EC2 접속용 private key 전체 내용 |

주의:

- `EC2_SSH_KEY`에는 `.pem` 파일 내용을 그대로 넣는다.
- private key는 코드에 커밋하면 안 된다.
- EC2에 IAM Role을 붙였으므로 GitHub Secrets에 AWS Access Key는 넣지 않는다.

## 4. EC2 최초 1회 준비

EC2에 SSH 접속한다.

```bash
ssh -i key.pem ubuntu@EC2_PUBLIC_IP
```

### 4-1. Java 21 설치

Ubuntu 기준:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

### 4-2. 실행 사용자 생성

```bash
sudo useradd -r -s /usr/sbin/nologin doody || true
sudo mkdir -p /opt/doody
sudo chown -R doody:doody /opt/doody
```

### 4-3. 환경변수 파일 생성

```bash
sudo nano /opt/doody/.env
```

아래 값을 실제 값으로 입력한다.

```env
SPRING_PROFILES_ACTIVE=prod
DB_HOST=your-db-host
DB_NAME=your-db-name
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
AI_ENGINE_BASE_URL=http://your-ai-engine-url
AWS_REGION=ap-northeast-2
S3_BUCKET=your-s3-bucket
S3_PUBLIC_BASE_URL=
HANA_AUTONOMY_URL=https://www.hanafn.com
HANA_CONNECTION_URL=https://www.hanafn.com
```

권한 설정:

```bash
sudo chown doody:doody /opt/doody/.env
sudo chmod 600 /opt/doody/.env
```

EC2에서는 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 넣지 않는다.  
S3 권한은 EC2에 연결된 IAM Role이 담당한다.

### 4-4. systemd 서비스 생성

```bash
sudo nano /etc/systemd/system/doody-backend.service
```

아래 내용을 넣는다.

```ini
[Unit]
Description=Doody Spring Backend
After=network.target

[Service]
User=doody
WorkingDirectory=/opt/doody
EnvironmentFile=/opt/doody/.env
ExecStart=/usr/bin/java -jar /opt/doody/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

서비스 등록:

```bash
sudo systemctl daemon-reload
sudo systemctl enable doody-backend
```

처음에는 jar가 없어서 start가 실패할 수 있다. GitHub Actions가 첫 배포로 `/opt/doody/app.jar`를 올린 뒤 재시작한다.

## 5. EC2 IAM Role 확인

EC2 인스턴스에 아래 Role이 붙어 있어야 한다.

```txt
doody-app-ec2-role
```

Role에는 아래 정책이 연결되어 있어야 한다.

```txt
DoodyMissionEvidenceS3Policy
```

권장 정책:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DoodyMissionEvidenceUpload",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/doody/mission/*"
    }
  ]
}
```

`YOUR_BUCKET_NAME`은 실제 `S3_BUCKET` 값으로 바꾼다.

## 6. 배포 실행

로컬에서 변경사항을 push한다.

```bash
git add .
git commit -m "feat: add github actions ec2 deploy"
git push origin main
```

GitHub에서 확인:

```txt
Repository -> Actions -> Deploy Spring Backend to EC2
```

## 7. 배포 확인

EC2에서 서비스 상태 확인:

```bash
sudo systemctl status doody-backend --no-pager
```

로그 확인:

```bash
journalctl -u doody-backend -f
```

헬스 체크:

```bash
curl http://localhost:8080/api/health
```

외부에서 확인:

```bash
curl http://EC2_PUBLIC_IP:8080/api/health
```

## 8. 자주 나는 오류

| 증상 | 원인 | 해결 |
|---|---|---|
| `Permission denied (publickey)` | GitHub Secret의 SSH key 오류 | `EC2_SSH_KEY`에 private key 전체 내용 확인 |
| `systemctl restart doody-backend` 실패 | systemd 서비스 미생성 | `/etc/systemd/system/doody-backend.service` 생성 |
| DB 연결 실패 | `.env` DB 값 오류 또는 보안그룹 문제 | DB 환경변수와 RDS inbound 확인 |
| S3 업로드 실패 | EC2 Role 또는 S3 정책 문제 | `doody-app-ec2-role`과 `DoodyMissionEvidenceS3Policy` 확인 |
| 외부 접속 안 됨 | EC2 보안그룹 8080 미개방 | inbound rule에 TCP 8080 허용 |

## 9. 운영 권장사항

- `AmazonS3FullAccess`는 제거하고 `DoodyMissionEvidenceS3Policy`만 유지한다.
- 운영에서는 8080을 직접 열기보다 Nginx + HTTPS를 앞단에 두는 것을 권장한다.
- GitHub Secrets에는 AWS Access Key를 넣지 않는다.
- EC2의 `.env` 파일은 `chmod 600`으로 보호한다.

