#! /bin/bash

# Author: Dave Grove
#
# Simple script to use svn copy to tag a X10 release
# 
# Usage: makeTag -rev <svn revision number> -tag <revision name>
#

while [ $# != 0 ]; do

  case $1 in
    -rev)
	export REVISION=$2
	shift
    ;;

    -tag)
	export TAG=$2
	shift
    ;;

    *)
	echo "unknown option: '$1'"
	exit 1
    ;;
   esac
   shift
done

if [[ -z "$REVISION" ]]; then
    echo "usage: $0 must give svn revision number as -rev <rev>"
    exit 1
fi

if [[ -z "$TAG" ]]; then
    echo "usage: $0 must give tag name as -tag <tag>"
    exit 1
fi

svn copy -r $REVISION https://x10.svn.sourceforge.net/svnroot/x10/trunk/ \
         https://x10.svn.sourceforge.net/svnroot/x10/tags/$TAG \
         -m "Tagging trunk revision $REVISION as $TAG release of X10"
svn copy -r $REVISION https://x10.svn.sourceforge.net/svnroot/x10/documentation/trunk/ \
         https://x10.svn.sourceforge.net/svnroot/x10/documentation/tags/$TAG \
         -m "Tagging documentation trunk revision $REVISION as $TAG release of X10"
svn copy -r $REVISION https://x10.svn.sourceforge.net/svnroot/x10/benchmarks/trunk/ \
         https://x10.svn.sourceforge.net/svnroot/x10/benchmarks/tags/$TAG \
         -m "Tagging benchmarks trunk revision $REVISION as $TAG release of X10"