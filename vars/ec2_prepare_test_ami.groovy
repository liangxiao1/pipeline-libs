// Build test AMIs from given COMPOSE_URL, the base AMIs are the 7.6,7.7,7.8,8.0,8.1
// Rememeber to keep them up to date.
// Below env's are required
// KEY_NAME=''(keypair name on aws)
// KEYFILE=''(ssh keyfile realpath)
// Below params are required
// params.EC2_REGION=''
// params.EC2_PROFILE=""
// params.EC2_SUBNET=""
// params.EC2_SG_GROUP=""
// params.PROXY_URL=""
// params.ARCH='x86_64|aarch64'

def call() {
    sh '''
    #!/bin/bash -x
    if [[ -e $WORKSPACE/job_env.txt ]]; then
        source $WORKSPACE/job_env.txt
    fi

    if [[ -z $JOB_BASE_AMI ]]; then
        echo "No JOB_BASE_AMI specified, auto select one!"
        source /home/ec2/p3_venv/bin/activate
        cd /home/ec2/mini_utils/
        ami_options=''
        if ! [ -z $ARCH ]; then
            ami_options=" -p ${ARCH}"
        fi
        if ! [ -z $BOOT_MODE ]; then
           ami_options="${ami_options} -m ${BOOT_MODE}"
        fi
        if ! [ -z $COMPOSE_ID ]; then
            python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s ami_id -d ${ami_options}
            ami_id=$(python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s ami_id ${ami_options})
            branch_name=$(python ec2_ami_select.py -f data/branch_map.yaml -c $COMPOSE_ID -s branch_name)
        elif ! [ -z $BRANCH_NAME ]; then
            python ec2_ami_select.py -f data/branch_map.yaml -c ${BRANCH_NAME} -s ami_id -d -p ${ami_options}
            ami_id=$(python ec2_ami_select.py -f data/branch_map.yaml -c ${BRANCH_NAME} -s ami_id -p ${ami_options})
            branch_name = ${BRANCH_NAME}
        else
            # sleep 60 to wait brew tag
            sleep 60
            python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s ami_id -d -p  ${ami_options}
            ami_id=$(python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s ami_id -p ${ami_options})
            branch_name=$(python ec2_ami_select.py -f data/branch_map.yaml -c "${JOB_PKGURL}${JOB_BREWTAG//' '}" -s branch_name)
        fi
        if ! [ -z $branch_name ]; then
            echo "BRANCH_NAME=${branch_name}" >> $WORKSPACE/job_env.txt
        fi
        deactivate
    else
        echo "Use specified $BASE_AMI"
        ami_id=$JOB_BASE_AMI
    fi

    ssh_user='ec2-user'

    if [[ $COMPOSE_ID =~ 'RHEL-7' ]]; then
        pkgs="install,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,ntpdate,perf,nvme-cli,pciutils,fio,git,tar,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,strace,sos,strace,acpid"
    elif [[ $COMPOSE_ID =~ 'RHEL-8' ]]; then
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace,acpid,mokutil,libfabric,openmpi,kernel-modules-extra,iproute-tc,libcgroup,bcc-tools,libbpf,wireshark-cli,tcpdump,nmap-ncat"
    elif [[ $COMPOSE_ID =~ 'RHEL-9' ]]; then
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,libaio-devel,blktrace,fio,nvme-cli,git,tar,nfs-utils,libvirt,qemu-kvm,python3,dracut-fips,kernel-debug,python3-pip,hostname,podman,xdp-tools,openssl-devel,glibc-all-langpacks,sos,strace,acpid,mokutil,libfabric,openmpi,kernel-modules-extra,iproute-tc,libcgroup,bcc-tools,libbpf,wireshark-cli,tcpdump,nmap-ncat,net-tools,tpm2-tools,tpm2-abrmd,acpica-tools,openmpi-devel,infiniband-diags,socat,acpica-tools"
    elif [[ $COMPOSE_ID =~ 'RHEL-10' ]]; then
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,libaio-devel,blktrace,fio,nvme-cli,git,tar,nfs-utils,libvirt,qemu-kvm,python3,dracut-fips,kernel-debug,python3-pip,hostname,podman,xdp-tools,openssl-devel,glibc-all-langpacks,sos,strace,acpid,mokutil,libfabric,openmpi,kernel-modules-extra,iproute-tc,libcgroup,bcc-tools,libbpf,wireshark-cli,tcpdump,nmap-ncat,net-tools,tpm2-tools,tpm2-abrmd,acpica-tools,openmpi-devel,infiniband-diags,socat,acpica-tools"
    elif [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
        if [[ $COMPOSE_ID =~ 'CentOS-Stream-8' ]]; then
            ssh_user='centos'
            echo "$ssh_user"
        fi
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3-pip,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace,virt-what,bcc-tools,libbpf,wireshark-cli,tcpdump,nmap-ncat"
    else
        pkgs="make,automake,autoconf,sysstat,gcc,unzip,wget,quota,bzip2,iperf3,pciutils,fio,psmisc,expect,perf,nvme-cli,pciutils,fio,php-cli,php-xml,php-json,libaio-devel,blktrace,fio,nvme-cli,git,tar,nfs-utils,libvirt,qemu-kvm,kernel-debug,python3,dracut-fips,podman,xdp-tools,openssl-devel,sos,strace,virt-what,wireshark-cli,tcpdump,nmap-ncat"
    fi
    if [[ ${ARCH} =~ 'aarch64' ]]; then
        waitseconds=800
        instance_type='t4g.medium'
    else
        waitseconds=600
        instance_type='t3.small'
    fi
    if [[ $COMPOSE_ID =~ '.n.' ]]; then
        echo "It is nightly build, wait ${waitseconds}s to make sure repo ready!"
        sleep ${waitseconds}
    fi
    repo_url=$REPO_URL
    source /home/ec2/p3_venv/bin/activate
    cd /home/ec2/mini_utils/
    build_cmd="python ec2_ami_build.py --profile ${EC2_PROFILE} --ami-id $ami_id  --key_name ${KEY_NAME} --security_group_ids ${EC2_SG_GROUP} \
        --region ${EC2_REGION} --subnet_id ${EC2_SUBNET} --tag ${VM_PREFIX}_${COMPOSE_ID}_${ARCH} --user $ssh_user --keyfile ${KEYFILE} \
        --proxy_url ${PROXY_URL} --instance_type $instance_type --new_ami $WORKSPACE/test_ami"
    
    if [ -z ${POST_CMDS} ]; then
        post_cmd='uname -r'
    else
        post_cmd=${POST_CMDS}
    fi

    if [[ $PKGS =~ 'certification' ]]; then
        pkgs="${PKGS}"
    elif ! [ -z $PKGS ]; then
        pkgs="${pkgs},${PKGS}"
    fi

    if ${UPDATE_BASEAMI}; then
        if ${IS_INSTALL_PKG_LIST}; then
            build_cmd="${build_cmd} --pkgs $pkgs"
        fi
    fi
    if ${ENABLE_CERTREPO}; then
        if ${IS_INSTALL_PKG_LIST}; then
            build_cmd="${build_cmd} --pkgs $pkgs --enable_certrepo"
        fi
    fi

    if ! ${UPDATE_BASEAMI}; then
        new_ami=$ami_id
        echo "Use baseami $new_ami directly in testing"
    elif ! [ -z $JOB_INFO_BUILD_ID ]; then
        $build_cmd --pkg_url $PKG_URL
    elif [[ -z $PKG_URL && $COMPOSEID_URL != "UNSPECIFIED" ]]; then
        $build_cmd --repo_url $repo_url --cmds "$post_cmd"
    elif [[ -z $PKG_URL && $COMPOSEID_URL == "UNSPECIFIED" ]]; then
        $build_cmd --cmds "$post_cmd"
    else
        $build_cmd --repo_url $repo_url --pkg_url $PKG_URL --cmds "$post_cmd"
    fi

    deactivate
    if ${UPDATE_BASEAMI}; then
        cat $WORKSPACE/test_ami
        new_ami=$(cat $WORKSPACE/test_ami)
        echo "Use $new_ami for test"
    fi
    echo "IMAGE=$new_ami" >> $WORKSPACE/job_env.txt
    echo "IMAGE: $new_ami" >> $WORKSPACE/job_env.yaml
    BUILDNAME="${COMPOSE_ID}_${new_ami}"
    echo "COMPOSE_ID: $COMPOSE_ID" >> $WORKSPACE/job_env.yaml
    echo "BUILDNAME=$BUILDNAME" >> $WORKSPACE/job_env.txt
    echo "BUILDNAME: $BUILDNAME" >> $WORKSPACE/job_env.yaml
    '''
}