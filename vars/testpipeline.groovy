def call(Map configMap) {

    pipeline {

        agent {
            label 'Java'
        }

        environment {
            appVersion = ""
            acc_id = "553490164630"
            region = "us-east-1"
            project = "${configMap.project}"
            component = "${configMap.component}"
        }

        options {
            timeout(time: 15, unit: 'MINUTES')
        }

        parameters {
            booleanParam(
                name: 'DEPLOY',
                defaultValue: false,
                description: 'Deploy application'
            )
        }


        stages {

            stage('Read version') {
                steps {
                    script {

                        def packageJson = readJSON file: 'package.json'

                        appVersion = packageJson.version

                        echo "Building version ${appVersion}"

                    }
                }
            }


            stage('Install Dependencies') {

                steps {

                    sh '''
                        npm install
                    '''

                }
            }


            stage('Unit tests') {

                steps {

                    script {

                        def result = sh(
                            script: 'npm test',
                            returnStatus: true
                        )


                        if(result != 0){

                            error "Unit tests failed"

                        }

                        else{

                            echo "Unit tests passed"

                        }

                    }

                }
            }


            stage('SonarQube Analysis') {

                steps {

                    script {

                        def scannerHome =
                        tool name: 'sonar-8.0'


                        withSonarQubeEnv('sonar-server') {

                            sh """
                            ${scannerHome}/bin/sonar-scanner
                            """

                        }

                    }

                }

            }



            stage('Quality Gate') {

                steps {

                    script {

                        timeout(time:1, unit:'HOURS') {


                            def qg = waitForQualityGate()


                            if(qg.status != 'OK'){

                                error "Quality Gate Failed ${qg.status}"

                            }


                        }

                    }

                }

            }



            stage('Build Image') {

                steps {

                    script {


                        withAWS(
                            credentials:'aws-creds',
                            region:"${region}"
                        ){


                            sh """

                            aws ecr get-login-password \
                            --region ${region} |
                            docker login \
                            --username AWS \
                            --password-stdin \
                            ${acc_id}.dkr.ecr.${region}.amazonaws.com


                            docker build \
                            -t ${acc_id}.dkr.ecr.${region}.amazonaws.com/${project}/${component}:${appVersion} .


                            docker push \
                            ${acc_id}.dkr.ecr.${region}.amazonaws.com/${project}/${component}:${appVersion}

                            """

                        }

                    }

                }

            }



            stage('Trivy OS Scan') {

                steps {

                    sh """

                    trivy image \
                    --severity HIGH,MEDIUM \
                    --format table \
                    ${acc_id}.dkr.ecr.${region}.amazonaws.com/${project}/${component}:${appVersion}

                    """

                }

            }



            stage('Trivy Dockerfile Scan') {

                steps {


                    sh """

                    trivy config \
                    --severity HIGH,MEDIUM \
                    Dockerfile

                    """

                }

            }




            stage('Push Image to ECR') {

                steps {

                    script {


                        withAWS(
                            credentials:'aws-creds',
                            region:"${region}"
                        ){

                            sh """

                            docker push \
                            ${acc_id}.dkr.ecr.${region}.amazonaws.com/${project}/${component}:${appVersion}

                            """

                        }

                    }

                }

            }


        }



        post {


            always {

                echo "Cleaning workspace"

                cleanWs()

            }


            success {

                echo "Pipeline Success"

            }


            failure {

                echo "Pipeline Failed"

            }

        }


    }

}