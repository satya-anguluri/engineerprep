# MarketplaceUi

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.1.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.


# AWS Step Function Demo Flow

This project includes a demo integration with AWS Step Functions to orchestrate and automate a multi-step business process. The Step Function was used to demonstrate how serverless workflows can coordinate AWS Lambda functions and other AWS services for the marketplace platform.

## Demo Flow Overview
- The Step Function defines a state machine that models the business process as a series of steps (states).
- Each step can invoke a Lambda function, make a service call, or perform a wait/choice operation.
- The demo flow was used to showcase:
	- Order processing or vendor onboarding automation
	- Error handling and retries
	- Parallel and sequential task execution
	- Integration with other AWS services (e.g., SNS, SQS, DynamoDB, etc.)

## How to Use
1. The Step Function state machine is defined in AWS Console or as a JSON/YAML file.
2. Each state in the flow is mapped to a Lambda function or AWS service integration.
3. The flow can be triggered manually from the AWS Console, via API Gateway, or programmatically from the application backend.
4. Execution results and state transitions can be monitored in the AWS Step Functions Console.

## Example Use Cases
- Automated order fulfillment
- Vendor onboarding workflow
- Payment and notification orchestration

For more details, see the AWS Step Functions documentation: https://docs.aws.amazon.com/step-functions/latest/dg/welcome.html


# AWS RDS (Relational Database Service)

This project uses Amazon RDS for managed MySQL database hosting in production. Key points:

- The RDS instance is used for persistent storage of marketplace data.
- Database credentials and endpoint are managed securely (e.g., via AWS Secrets Manager or Parameter Store).
- Security group rules must allow inbound connections from trusted sources (e.g., EC2, Fargate tasks, or your public IP for admin access).
- For local access (e.g., DBeaver), add your public IP to the RDS security group.
- SSL connections are supported and recommended for secure data transfer.

For more information, see: https://docs.aws.amazon.com/rds/

# AWS ECS Fargate Deployment

The backend services for this project are deployed using Amazon ECS with Fargate launch type. Key points:

- ECS Fargate provides serverless container hosting, removing the need to manage EC2 instances.
- Task definitions specify the container image, resource requirements, and environment variables.
- Services are deployed in a cluster and can be integrated with an Application Load Balancer (ALB) for traffic routing.
- Deployments are managed via the AWS Console, CLI, or CI/CD pipelines (e.g., GitHub Actions).
- Circuit breaker and health checks are configured for safe, automated rollbacks on deployment failures.

For more information, see: https://docs.aws.amazon.com/ecs/latest/developerguide/ecs-fargate.html
