
def call(Map configMap) {

    pipeline {
        agent {
            label 'Java'
        }

        stages {

            stage('Build') {
                steps {
                    echo "Project  : ${configMap.project}"
                    echo "Component: ${configMap.component}"
                }
            }

        }
    }

}