#! /bin/sh

rm -rf tmp
mkdir -p tmp

function gif2tga()
{
    bname=$1
    shift
    images=$1
    shift
    xysize=$1
    shift

    i=1 ; 
    while [ $i -le $images ] ; do 
        giftopnm -image $i $bname.gif > tmp/$bname.$i.pnm
        pamscale -xsize=$xysize -ysize=$xysize tmp/$bname.$i.pnm > tmp/$bname.$xysize"x"$xysize.$i.pam 
        pamtotga -rgb -norle tmp/$bname.$xysize"x"$xysize.$i.pam > $bname.$xysize"x"$xysize.$i.tga 
        let i=$i+1
    done
}

gif2tga bob2 33 64
gif2tga bob2 33 128
gif2tga bob2 33 256
gif2tga bob2 33 512

