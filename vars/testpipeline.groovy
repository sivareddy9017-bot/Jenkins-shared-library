def call(Map configMap){
    pipeline {
    agent {
        node {
            label 'Java' 
        } 
    } 

       stages{
        stage('Build'){
            steps{
                echo "build"
            }
        }
       }
       stage('test'){
            steps{
                echo "test"
            }
        }
       }
}