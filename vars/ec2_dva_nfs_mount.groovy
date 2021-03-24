// mount nfs log server to save log
// NFS_SERVER=xxx
// NFS_PATH_DIR=xxx
// NFS_MOUNT_POINT=xxx
def call() {
    sh '''
    if ! [[ -d ${NFS_MOUNT_POINT} ]]; then
        echo "${NFS_MOUNT_POINT} not found, create it"
        mkdir ${NFS_MOUNT_POINT}
    fi
    if ! mount|grep ${NFS_MOUNT_POINT}; then
        echo "nfs not mount, mount it"
        sudo mount  ${NFS_SERVER}:${NFS_PATH_DIR} ${NFS_MOUNT_POINT}
    fi
    dva_cfg=`mktemp --suffix _dva.yaml -p ${WORKSPACE}`
    cd ${WORKSPACE}
    echo "DVA_CFG: \"$dva_cfg\"" >> "job_env.yaml"
    echo "DVA_CFG=$dva_cfg" >> "job_env.txt"
    cp -r /tmp/nfs_dva/ami_val/data ${WORKSPACE}
    sudo chmod 600 ${WORKSPACE}/data/virtqe_s1.pem
    '''
}