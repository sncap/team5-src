node {
  stage('========== Clone repository ==========') {
    checkout scm
  }
  stage('========== Build image ==========') {
    //app = docker.build("jenkins-docker-pipeline/my-image")
    BUILD_FULL = sh (
       script: "./swa/mk_img.sh ",
       returnStatus: true
    ) == 0
    echo "Build full flag: ${BUILD_FULL}"
  }
  //stage('========== Push image ==========') {
  //  docker.withRegistry('YOUR_REGISTRY', 'YOUR_CREDENTIAL') {
  //    app.push("${env.BUILD_NUMBER}")
  //    app.push("latest")
  //  }
  //}
}
