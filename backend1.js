const express = require('express')
const { use } = require('express/lib/application')
const mongoose = require('mongoose')
const http = require('http')
const app = express()
const server = http.createServer(app)
const io = require('socket.io')(server)

const MONGO_URL = 'mongodb://127.0.0.1:27017/test' // 이 부분 ssh로 접속한 서버 ip로 바꿔야함. 그 전에 ssh 서버에 몽고디비 깔아놓기도 하고!
const userSchema = new mongoose.Schema({
  name:String,
  stat:[Number]
})
const User = mongoose.model("arraytest", userSchema)
const { handle } = require('express/lib/router')
mongoose
  .connect(MONGO_URL)
  .then(() => console.log('MongoDB conected'))
  .catch((err) => {
    console.log(err);
  });


const randomstring = require('randomstring')
const { emit } = require('process')
const l=3,threshold=1;
let rcnt=[],pnames=[],isroomover=[],isroomoccupied=[],readycnt=[],x=[],y=[],z=[],rank=[],isdead=[]

// 소켓 연결 코드
io.sockets.on('connection', (socket) => {
  console.log(`Socket connected : ${socket.id}`)
  let myroomname,myusername;
  let is_in_game=false;
  socket.on('getmystatus',(data)=>{
    const receivedata = JSON.parse(data);
    const username = receivedata.username;
    let detail="",win,lose
    is_in_game =false
    User.findOne({ 'name': username }, function (err, person) {
          if (err) return handleError(err);
          if(person==null){
            detail="newbie"
            User.create({'name':username, win:0,lose:0})
            win=lose=0;
          }
          else{
            win=person.win;
            lose=person.lose;
          }
          console.log(`getstatus ${username} : %d win %d lose`, win, lose);
          const msg={
            username:username,
            win:win,
            lose:lose
          }
          io.emit('yourstatus',JSON.stringify(msg));
      })
  })
  socket.on('getranking',(data)=>{
    const receivedata = JSON.parse(data);
    const username = receivedata.username;
    is_in_game=false;
    User.find({},(err,docs)=>{
      if(err)return handleError(err);
      const msg={
        username:username,
        stats:docs,
        length:docs.length
      }
      io.emit('yourranking',JSON.stringify(msg));
      console.log(`${docs.length} docs have been transferred`)
    })
  })

  socket.on('enter', (data) => {
    const roomData = JSON.parse(data)
    const username = roomData.username
    const roomname = roomData.roomname
    myroomname = roomname
    myusername = username
    
    if(isroomoccupied[roomname]==true){
      const msg1 = {
        username : username
      }
      io.emit('refuse',JSON.stringify(msg1))
      console.log(`refuse ${username}`)
      return;
    }
    socket.join(`${roomname}`)
    console.log(`[Username : ${username}] entered [room name : ${roomname}]`)
    if(rcnt[roomname]==undefined)rcnt[roomname]=0;
    if(pnames[roomname]==undefined)pnames[roomname]=[];
    pnames[roomname][rcnt[roomname]]=username;
    msg_pnames=pnames[roomname];
    rank[username]=1;
    isdead[username]=false;
    rcnt[roomname]++;
    if(rcnt[roomname]>=l){
        console.log(`room full, game start`);
        isroomoccupied[roomname]=true;
        const msg = {
            pnames :msg_pnames
        }
        console.log(pnames[roomname]);
        readycnt[roomname]=0;
        io.to(`${roomname}`).emit('roomfound',JSON.stringify(msg));
    }
  })

  socket.on('ready',(data)=>{
    const roomData = JSON.parse(data)
    const username = roomData.username
    const roomname = roomData.roomname
    console.log(`[Username : ${username}] is ready! [room name : ${roomname}]`)
    readycnt[roomname]++;
    x[username]=roomData.x;
    y[username]=roomData.y;
    z[username]=roomData.z;
    is_in_game=true;
    if(readycnt[roomname]>=l){
        readycnt[roomname]=0;
        console.log(`all ready`);
        isroomover[roomname]=false;//시작
        const msg = {
          username:myusername
        }
        console.log(`game start!`)
        io.to(`${roomname}`).emit('accept',JSON.stringify(msg));
    }
  })

  socket.on('position',(data) => {
    const Data=JSON.parse(data);
    const username = myusername;
    x[username]=Data.x
    y[username]=Data.y
    z[username]=Data.z
  })

  socket.on('shoot',(data) => {
    const Data=JSON.parse(data);
    const username = myusername;
    const roomname = myroomname;
    const phi = Data.phi;
    const theta = Data.theta;
    Math.log(`phi : ${phi} , theta : ${theta}`);
    let victim=handleshoot(username,roomname,phi,theta);
    if(victim==null)return;
    const msg={
      username:username,
      victim:victim
    }
    io.to(`${roomname}`).emit('result',JSON.stringify(msg));
  })

  socket.on('newMessage', (data) => {
    const messageData = JSON.parse(data)
    console.log(`[Room name ${messageData.roomname}] ${messageData.username} : ${messageData.content}`)
    io.to(`${messageData.roomname}`).emit('update', JSON.stringify(messageData))
  })

  socket.on('disconnect', () => {
    console.log(`Socket disconnected : ${socket.id}`)
    socket.leave(`${myroomname}`)
    if(is_in_game===false)return;
    isdead[myusername]=true;
    rank[myusername]=rcnt[myroomname];
    rcnt[myroomname]--;
    console.log(`${myusername} left room ${myroomname}`);
    console.log(`rcnt : ${rcnt[myroomname]}`);
    if(rcnt[myroomname]<=0){
        rcnt[myroomname]=0;
        console.log(`${myroomname} exploded`);
        isroomoccupied[myroomname]=false;
        return;
    }
    if(rcnt[myroomname]>1)return;
    const endmsg={}
    io.to(`${myroomname}`).emit(`gameover`,JSON.stringify(endmsg));
    handlegameover(myroomname);
    
  })
})

server.listen(80, () => {
  console.log(`Server listening at http://localhost:80`)
})

function handlegameover(roomname){
  let arr=[];
  for(i=0;i<l;i++){
    User.findOne({"name":pnames[roomname][i]},function (err, person) {
      if (err) return handleError(err);
      for(j=0;j<l;j++)arr[j]=0;
      if(person==null){
        arr[rank[pnames[roomname][i]]-1]++;
        User.updateOne({'name':pnames[roomname][i]},{stat:arr},(err,docs)=>{
          if(err)return handleError(err);
        })
      }
      else{
        arr=person.stat;
        arr[rank[pnames[roomname][i]]-1]++;
        User.updateOne({'name':pnames[roomname][i]},{stat:arr},(err,docs)=>{
          if(err)return handleError(err);
        })
      }
    })
  }
}

function handleshoot(name,roomname,phi,theta){
  let d=1e9,victim=null;
  let dphi = phi *180 / Math.PI;
  let dtheta = theta * 180 / Math.PI;
  console.log(`deg : ${dphi} : ${dtheta}`);

  u=[Math.sin(theta)*Math.cos(phi),Math.sin(theta)*Math.sin(phi),Math.cos(theta)];
  origin=[x[name],y[name],z[name]]
  for(i=0;i<l;i++){
    person=pnames[roomname][i];
    if(person==name)continue;
    if(isdead[person])continue;
    v=sub([x[person],y[person],z[person]],origin);
    if(prod(u,v)<0)continue;
    proj=mul(prod(u,v),u)
    let mg = mag(sub(v,proj));
    console.log(`mag : ${mg}`);
    if(mag(sub(v,proj))>threshold)continue;
    if(d>mag(proj)){
      victim=person;
    }
  }
  console.log(`victim : ${victim}`);
  return victim;
}

function add(x,y){
  return [x[0]+y[0],x[1]+y[1],x[2]+y[2]];
}

function sub(x,y){ //x-y
  return [x[0]-y[0],x[1]-y[1],x[2]-y[2]];
}

function mag(x){
  return Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
}

function prod(x,y){
  return x[0]*y[0]+x[1]*y[1]+x[2]*y[2];
}

function mul(c,x){
  return [c*x[0],c*x[1],c*x[2]];
}