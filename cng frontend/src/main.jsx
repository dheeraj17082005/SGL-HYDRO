import React, {useEffect,useRef,useState} from 'react';
import {createRoot} from 'react-dom/client';
import './styles.css';
import {authApi, vehicleApi, dashboardApi, stationApi, alertApi, auditApi, journeyApi, anprApi, toVehicleView} from './api/services.js';

const demoVehicles=[...Array.from({length:24},(_,i)=>({plate:`MH12DE${String(1001+i).padStart(4,'0')}`,hydro:'VALID'})),...Array.from({length:12},(_,i)=>({plate:`MH12DE${String(1076+i).padStart(4,'0')}`,hydro:'EXPIRED'}))];
const backendLocalDateTime=()=>new Date().toISOString().replace(/\.\d{3}Z$/,'');
const nav=[['Overview','◫'],['Live Monitoring','◉'],['Bays & Queue','⇄'],['Vehicle Verification','▣'],['Alerts','⚑'],['Compliance','▤'],['Audit Logs','≡']];
function Badge({children,tone='neutral'}){return <span className={`badge ${tone}`}>{children}</span>}
function Sidebar({page,setPage,open,onClose}){const groups=[['OPERATIONS',[['Overview','⌂'],['Live Monitoring','◉'],['Bays & Queue','⇄']]],['VEHICLES',[['Vehicle Verification','▣'],['Compliance','▤']]],['CONTROL',[['Alerts','♧'],['Audit Logs','≡']]]];return <><button className={'sidebar-backdrop '+(open?'is-visible':'')} onClick={onClose} aria-label="Close navigation" tabIndex={open?0:-1}/><aside className={"sidebar "+(open?'is-open':'is-closed')} aria-hidden={!open} inert={!open}><div className="brand"><div className="brand-mark"><b>S</b><span>GAS</span></div><div className="brand-name">SABARMATI GAS<span>LIMITED</span></div><button className="sidebar-close" onClick={onClose} aria-label="Close navigation">×</button></div>{groups.map(([group,items])=><section className="nav-group" key={group}><div className="site-label">{group}</div><nav>{items.map(([name,icon])=><button key={name} onClick={()=>setPage(name)} className={page===name?'selected':''}><span className="navicon">{icon}</span><span>{name}</span></button>)}</nav></section>)}<div className="sidebar-bottom"><div className="sidebar-tagline"><span>✦</span><b>CNG for a<br/>Cleaner, Safer<br/>Tomorrow</b></div><div className="side-user"><div className="avatar">{(localStorage.getItem('sgl_username')||'OP').slice(0,2).toUpperCase()}</div><div>{localStorage.getItem('sgl_username')||'Station operator'}<small>{localStorage.getItem('sgl_user_role')||'Operator'}</small></div><span className="more">⋮</span></div></div></aside></>}
function Topbar({page,onLogout,station,stations,onStationChange,navOpen,onToggleNav,scrolled,onAlerts,onSearch}){const [now,setNow]=useState(()=>new Date());useEffect(()=>{const timer=setInterval(()=>setNow(new Date()),1000);return()=>clearInterval(timer)},[]);const time=now.toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false,timeZone:'Asia/Kolkata'});const date=now.toLocaleDateString('en-GB',{weekday:'short',day:'2-digit',month:'short',year:'numeric',timeZone:'Asia/Kolkata'});return <header className={"topbar "+(scrolled?'is-scrolled':'')}><div className="topbar-leading"><button className={"menu-toggle "+(navOpen?'is-open':'')} onClick={onToggleNav} aria-label={navOpen?'Hide side navigation':'Show side navigation'} aria-expanded={navOpen}>☰</button><span className="location-pin">⌖</span>{stations?.length>1?<select className="station-select station-dropdown topbar-station-select" aria-label="Select station" value={station?.id||''} onChange={e=>onStationChange?.(e.target.value)}>{stations.map(item=><option key={item.id} value={item.id}>{item.name}{item.code?' · '+item.code:''}</option>)}</select>:<strong className="topbar-station-name">{station?.name||'SGL Main CNG Station'}</strong>}</div><div className="top-actions"><div className="clock"><small>{date}</small><b>{time}</b></div><button className="topbar-icon" onClick={onSearch} aria-label="Search vehicles" title="Search vehicles">⌕</button><button className="topbar-icon notification-button" onClick={onAlerts} aria-label="Open alerts" title="Open alerts">♧</button><button className="profile-button" onClick={onLogout} title="Sign out" aria-label={'Sign out, '+(localStorage.getItem('sgl_username')||'operator')}>{(localStorage.getItem('sgl_username')||'OP').slice(0,2).toUpperCase()}<span>Sign out</span></button></div></header>}
function PageTitle({eyebrow,title,sub,action}){return <div className="page-title"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1>{sub&&<p>{sub}</p>}</div>{action}</div>}
function Metrics({items}){return <div className="metrics">{items.map((m,i)=><div className="metric" key={m.label}><div className="metric-head">{m.label}<span>{m.symbol||'↗'}</span></div><strong>{m.value}</strong><small className={m.tone||''}>{m.note}</small>{i===0&&<div className="metric-spark"><span/></div>}</div>)}</div>}
function Plate({plate='GJ00XX0000'}){return <span className="plate" aria-label={`Registration ${plate}`}><i>IND</i><b>{plate}</b></span>}
function VerificationCard({vehicle}){if(!vehicle)return null;const checked=vehicle.verifiedAt?new Date(vehicle.verifiedAt).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit',second:'2-digit'}):'Just now';return <section className="verify-card"><div className="verify-heading"><div><div className="eyebrow">VEHICLE VERIFICATION</div><h2><Plate plate={vehicle.plate}/></h2></div></div><div className="vehicle-name">{vehicle.make} <span>·</span> {vehicle.fuel}</div>{vehicle.owner&&<div className="verify-foot"><span>Owner <b>{vehicle.owner}</b></span>{vehicle.rto&&<span>RTO <b>{vehicle.rto}</b></span>}</div>}<div className={`decision ${vehicle.decision==='CLEARED'?'decision-pass':'decision-fail'}`}><div><strong>{vehicle.decision==='CLEARED'?'CLEARED FOR REFUELING':'REFUELING BLOCKED'}</strong>{vehicle.reason&&<small>{vehicle.reason}</small>}</div></div><div className="compliance-grid">{Object.entries(vehicle.checks||{}).map(([name,state])=><div className="compliance-item" key={name}><span>{name}</span><Badge tone={state==='VALID'?'pass':state==='UNKNOWN'||state==='MISSING'?'unknown':'fail'}>{state}</Badge></div>)}</div>{vehicle.hydroExpiry&&<div className="verify-foot"><span>Hydro-test expiry <b>{vehicle.hydroExpiry}</b></span>{vehicle.certificate&&<span>Certificate <b>{vehicle.certificate}</b></span>}</div>}<div className="verify-foot"><span>Source <b>{vehicle.source||'CNG REGISTRY'}</b></span><span>Checked <b>{checked}</b></span></div>{vehicle.decision==='CLEARED'&&<><button className="btn-outline certificate-print" onClick={()=>window.print()}>Print compliance certificate ↓</button><div className="print-certificate"><div className="certificate-brand">SABARMATI GAS LIMITED <span>STATION COMPLIANCE RECORD</span></div><h1>CNG Vehicle Compliance Certificate</h1><p>This record confirms the backend eligibility decision at the time shown below.</p><div className="certificate-vehicle"><span>VEHICLE REGISTRATION</span><strong>{vehicle.plate}</strong><small>{vehicle.make} · {vehicle.fuel}</small></div><p><b>Decision: CLEARED FOR REFUELING</b></p><div className="certificate-checks">{Object.entries(vehicle.checks||{}).map(([name,state])=><div key={name}><span>{name}</span><b>{state}</b></div>)}</div><p>Hydro-test expiry: {vehicle.hydroExpiry||'Not supplied'} · Certificate: {vehicle.certificate||'Not supplied'}</p><p>Verified at: {vehicle.verifiedAt||new Date().toISOString()} · Source: {vehicle.source||'CNG REGISTRY'}</p><small>Generated from the SGL station operations console. This is a digital compliance record, not a replacement for statutory source documents.</small></div></>}</section>}
function ErrorState({onRetry,message}){return <div className="error-state"><div>!</div><b>Verification unavailable</b><small>{message||'The vehicle registry did not respond. Do not refuel until records can be confirmed.'}</small><button className="btn-outline" onClick={onRetry}>Try again</button></div>}
function CameraMonitor({compact=false}){return <div className={`camera ${compact?'compact':''}`}><div className="camera-feed"><div className="feed-grid"/><div className="camera-top"><span className="camera-label"><i className="live-dot"/> CAM 01 · ENTRY LANE</span><span className="feed-online">SAMPLE LANE VIEW</span></div><div className="cam-crosshair">+</div><div className="lane-mark lane-a"/><div className="lane-mark lane-b"/><div className="vehicle-shape"><div className="windshield"/><div className="wheel w1"/><div className="wheel w2"/><div className="det-box"><span>ILLUSTRATIVE VEHICLE OVERLAY</span></div><div className="plate-box"><span>SAMPLE REGISTRATION</span><Plate plate="GJ00XX0000"/></div></div><div className="cam-bottom"><span>LANE PREVIEW <b>Illustration</b></span><span>SAMPLE VIEW</span></div></div><div className="camera-controls"><span><i className="live-dot"/> Illustrative lane preview</span><span>CAM-01&nbsp; · &nbsp;Entry lane <button>⛶</button></span></div></div>}
function Timeline(){return <div className="timeline">{[['10:42:18','Vehicle detected','Entry lane · CAM-01'],['10:42:19','Plate recognized','GJ00XX0000 · OCR 94%'],['10:42:19','Vehicle record retrieved','Live API · 248 ms'],['10:42:20','Compliance verified','All mandatory checks passed']].map(([time,title,detail],i)=><div className="timeline-row" key={time+title}><span className="time">{time}</span><span className={`timeline-dot ${i===3?'last':''}`}/><span><b>{title}</b><small>{i===1?<><Plate plate="GJ00XX0000"/> · OCR 94%</>:detail}</small></span></div>)}</div>}
function LivePage({stationId,setPage}){
 const videoRef=useRef(null),canvasRef=useRef(null),streamRef=useRef(null),processingRef=useRef(false),matchesRef=useRef([]),cooldownRef=useRef(new Map()),fileInputRef=useRef(null);
 const [mode,setMode]=useState('ENTRY'),[camera,setCamera]=useState('idle'),[busy,setBusy]=useState(false),[result,setResult]=useState(null),[message,setMessage]=useState('Upload an Indian number plate photo or start the live camera to begin.'),[candidate,setCandidate]=useState(''),[history,setHistory]=useState([]);
 const [feedTab,setFeedTab]=useState('UPLOAD'),[uploadedImage,setUploadedImage]=useState(null),[uploadedFile,setUploadedFile]=useState(null),[uploadedFileName,setUploadedFileName]=useState(''),[uploadError,setUploadError]=useState(''),[selectedSample,setSelectedSample]=useState(null);
 const [detectedBbox,setDetectedBbox]=useState(null),[liveScanStatus,setLiveScanStatus]=useState('READY');

 async function processDetection(plate,confidence,rawTimestamp,detectionSource='CAMERA'){
  setCandidate(plate);
  setMessage(`Plate ${plate} confirmed (${Math.round(confidence*100)}%). Checking compliance and adding to fueling queue...`);
  const all=await journeyApi.list(stationId);
  const journeys=Array.isArray(all)?all:Array.isArray(all?.content)?all.content:[];
  const active=journeys.find(j=>String(j.registrationNumber||'').replace(/[\s-]/g,'').toUpperCase()===plate&&String(j.status).toUpperCase()!=='EXITED');
  let out=null;
  if(mode==='ENTRY'){
   if(active){
    out={plate,confidence,journeyId:active.id,status:active.complianceStatus,journeyStatus:active.status,duplicate:true};
    setMessage(`Vehicle ${plate} already has active station journey #${active.id} (${active.status}). No duplicate created.`);
   } else {
    const created=await anprApi.ingest({stationId:Number(stationId),cameraId:Number(import.meta.env.VITE_CAMERA_ID)||1,registrationNumber:plate,detectedAt:rawTimestamp||backendLocalDateTime()});
    const createdId=created.journeyId??created.id;
    let recorded=created;
    const createdStatus=String(created.journeyStatus||created.status||'').toUpperCase();
    if(String(created.complianceStatus||'').toUpperCase()==='ELIGIBLE'&&createdId&&createdStatus==='ENTERED')recorded=await journeyApi.enterQueue(createdId);
    out={plate,confidence,journeyId:createdId,status:created.complianceStatus,journeyStatus:recorded.journeyStatus||recorded.status||created.journeyStatus||created.status};
    setMessage(String(created.complianceStatus).toUpperCase()==='ELIGIBLE'?`✅ Vehicle ${plate} verified & added to station fueling queue (#${createdId})!`:`⚠️ Vehicle ${plate} recorded, but fueling is blocked by compliance.`);
   }
  } else {
   if(!active){out={plate,confidence,status:'NOT_FOUND',journeyStatus:'NO ACTIVE JOURNEY'};setMessage(`No active journey found for vehicle ${plate}.`);}
   else if(active.status==='FUELING'){await journeyApi.completeFueling(active.id);const updated=await journeyApi.exit(active.id);out={plate,confidence,journeyId:updated.id,status:updated.complianceStatus,journeyStatus:updated.status};setMessage(`Exit recorded for ${plate}. Fueling marked complete.`);}
   else if(active.status==='FUELING_COMPLETED'){const updated=await journeyApi.exit(active.id);out={plate,confidence,journeyId:updated.id,status:updated.complianceStatus,journeyStatus:updated.status};setMessage(`Vehicle ${plate} exited station successfully.`);}
   else {out={plate,confidence,journeyId:active.id,status:active.complianceStatus,journeyStatus:active.status};setMessage(`Vehicle ${plate} is not ready to exit. Current stage: ${active.status}.`);}
  }
  setResult(out);
  setHistory(old=>[{...out,time:new Date().toLocaleTimeString('en-GB'),mode:detectionSource},...old].slice(0,6));
  return out;
 }

 async function handleFileUpload(file){
  if(!file)return;
  setUploadError('');
  setBusy(true);
  setCandidate('');
  setResult(null);
  setUploadedFile(file);
  setUploadedFileName(file.name||'plate_photo.jpg');
  const previewUrl=URL.createObjectURL(file);
  setUploadedImage(previewUrl);
  setMessage(`Analyzing ${file.name||'photo'} with YOLOv8 detector & EasyOCR...`);
  try{
   const read=await anprApi.recognize(file);
   const plate=read.plateDetected&&read.registrationNumber?String(read.registrationNumber).replace(/[\s-]/g,'').toUpperCase():'';
   const confidence=Number(read.confidence)||0;
   if(!plate||confidence<0.15){
    setCandidate('');
    setResult(null);
    setUploadError(read.reason==='NO_PLATE_DETECTED'?'No license plate detected in this photo. Make sure the number plate is front/rear facing and clearly visible.':`Plate reading confidence too low (${Math.round(confidence*100)}%). Reason: ${read.reason||'UNREADABLE'}.`);
    setMessage(`⚠️ No valid plate detected in ${file.name||'image'}. Try another photo or one of the quick test samples below.`);
    return;
   }
   await processDetection(plate,confidence,read.timestamp,'IMAGE UPLOAD');
  }catch(err){
   setUploadError(err.message||'Image recognition failed.');
   setMessage(`Recognition error: ${err.message}`);
  }finally{
   setBusy(false);
  }
 }

 async function handleSampleSelect(samplePath,plateLabel){
  setSelectedSample(samplePath);
  setBusy(true);
  setMessage(`Loading sample plate ${plateLabel}...`);
  try{
   const resp=await fetch(samplePath);
   const blob=await resp.blob();
   const file=new File([blob],samplePath.split('/').pop(),{type:'image/jpeg'});
   await handleFileUpload(file);
  }catch(err){
   setUploadError(`Failed to load sample: ${err.message}`);
   setBusy(false);
  }
 }

 async function startCamera(){
  if(camera==='starting'||camera==='live')return;
  setCamera('starting');
  setLiveScanStatus('CONNECTING');
  setMessage('Allow camera access when your browser asks. Connecting to the camera…');
  try{
   if(!window.isSecureContext&&window.location.hostname!=='localhost'&&window.location.hostname!=='127.0.0.1')throw new Error('This page cannot access a camera. Open it from localhost or a secure https address.');
   let stream;
   const requests=[{video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720},frameRate:{ideal:24,max:30}},audio:false},{video:true,audio:false}];
   let lastError;
   for(const constraints of requests){
    let expired=false,timerId;
    try{
     const pending=navigator.mediaDevices.getUserMedia(constraints).then(acquired=>{if(expired){acquired.getTracks().forEach(track=>track.stop());throw new Error('Late camera stream stopped.');}return acquired;});
     stream=await Promise.race([pending,new Promise((_,reject)=>{timerId=setTimeout(()=>{expired=true;reject(new Error('Camera access timed out. Check site camera permission in browser settings.'))},15000)})]);
     clearTimeout(timerId);break;
    }catch(error){clearTimeout(timerId);lastError=error;if(['NotAllowedError','PermissionDeniedError','NotReadableError'].includes(error.name)||String(error.message).includes('did not respond'))throw error;}
   }
   if(!stream)throw lastError||new Error('No camera stream was returned.');
   streamRef.current=stream;
   const track=stream.getVideoTracks()[0];
   const video=videoRef.current;
   if(!video){stream.getTracks().forEach(track=>track.stop());throw new Error('Camera preview not ready. Refresh page and try again.');}
   video.srcObject=stream;
   await Promise.race([video.play(),new Promise((_,reject)=>setTimeout(()=>reject(new Error('Camera opened but video feed did not start.')),8000))]);
   setCamera('live');
   setLiveScanStatus('SCANNING');
   setMessage(`Camera connected (${track?.getSettings?.().width||'auto'}×${track?.getSettings?.().height||'auto'}). Scanning for vehicle license plate…`);
  }catch(e){
   streamRef.current?.getTracks().forEach(track=>track.stop());streamRef.current=null;setCamera('error');
   setLiveScanStatus('OFFLINE');
   const errors={NotAllowedError:'Camera permission blocked. Allow camera access in browser settings.',PermissionDeniedError:'Camera permission blocked. Allow camera access in browser settings.',NotFoundError:'No camera found. Connect a camera, then try again.',NotReadableError:'Camera busy in another app. Close other apps and try again.'};
   setMessage(errors[e.name]||e.message||'Camera could not be started.');
  }
 }
 function stopCamera(){streamRef.current?.getTracks().forEach(track=>track.stop());streamRef.current=null;if(videoRef.current)videoRef.current.srcObject=null;setCamera('idle');setBusy(false);setDetectedBbox(null);setLiveScanStatus('READY');}
 useEffect(()=>{if(camera==='live'&&streamRef.current&&videoRef.current){videoRef.current.srcObject=streamRef.current;videoRef.current.play().catch(()=>setMessage('Camera is ready but video could not start.'))}},[camera]);
 useEffect(()=>()=>streamRef.current?.getTracks().forEach(track=>track.stop()),[]);
 useEffect(()=>{matchesRef.current=[];setCandidate('');setMessage(mode==='ENTRY'?'Entry mode: eligible detections are verified and added to fueling queue.':'Exit mode: detections match and close an existing station journey.')},[mode]);
 useEffect(()=>{
  if(camera!=='live'||feedTab!=='CAMERA')return;
  let stopped=false;
  const timer=setInterval(async()=>{
   if(stopped||processingRef.current||!videoRef.current||videoRef.current.readyState<2||!videoRef.current.videoWidth)return;
   processingRef.current=true;setBusy(true);
   try{
    const video=videoRef.current,canvas=canvasRef.current;
    if(!canvas){processingRef.current=false;setBusy(false);return;}
    const captureScale=Math.min(1,1280/video.videoWidth);
    canvas.width=Math.round(video.videoWidth*captureScale);
    canvas.height=Math.round(video.videoHeight*captureScale);
    const context=canvas.getContext('2d',{alpha:false});
    context.drawImage(video,0,0,canvas.width,canvas.height);
    const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',0.85));
    if(!blob){processingRef.current=false;setBusy(false);return;}
    const frame=new File([blob],'webcam-frame.jpg',{type:'image/jpeg'});
    const read=await anprApi.recognize(frame);
    if(read.bbox){
     setDetectedBbox(read.bbox);
    }else{
     setDetectedBbox(null);
    }
    const plate=read.plateDetected&&read.registrationNumber?String(read.registrationNumber).replace(/[\s-]/g,'').toUpperCase():'';
    const confidence=Number(read.confidence)||0;
    if(!plate||confidence<0.15){
     const recent=matchesRef.current;
     if(recent.length&&Date.now()-recent.at(-1).at<25000){
      setLiveScanStatus(`HOLDING ${recent.at(-1).plate}`);
      setMessage(`Holding ${recent.at(-1).plate} steady… analyzing plate characters.`);
      return;
     }
     matchesRef.current=[];setCandidate('');
     setLiveScanStatus(read.plateDetected?'PLATE IN VIEW · READING':'SCANNING FOR PLATE');
     setMessage(read.plateDetected?'Plate detected in view! Reading characters, hold steady...':'Scanning live feed for vehicle license plate...');
     return;
    }
    setCandidate(plate);
    setLiveScanStatus(`RECOGNIZED: ${plate}`);
    let recent=matchesRef.current.filter(match=>Date.now()-match.at<30000);
    if(recent.length&&recent.at(-1).plate===plate)recent.push({plate,confidence,at:Date.now()});
    else recent=[{plate,confidence,at:Date.now()}];
    matchesRef.current=recent.slice(-2);
    const needed=confidence>=0.25?1:2;
    if(recent.length<needed){setMessage(`Detected ${plate} (${Math.round(confidence*100)}%)… confirming read.`);return;}
    if(Date.now()-(cooldownRef.current.get(plate)||0)<20000){setMessage(`${plate} was recently processed. Holding duplicate.`);return;}
    cooldownRef.current.set(plate,Date.now());
    matchesRef.current=[];
    await processDetection(plate,confidence,read.timestamp,'CAMERA');
   }catch(e){setMessage(e.message||'Plate processing failed.')}finally{processingRef.current=false;setBusy(false)}
  },400);
  return()=>{stopped=true;clearInterval(timer)};
 },[camera,mode,stationId,feedTab]);

 return <LiveMonitoringDashboard stationId={stationId} setPage={setPage} mode={mode} setMode={setMode} camera={camera} busy={busy} result={result} message={message} candidate={candidate} history={history} videoRef={videoRef} canvasRef={canvasRef} detectedBbox={detectedBbox} liveScanStatus={liveScanStatus} startCamera={startCamera} stopCamera={stopCamera} feedTab={feedTab} setFeedTab={setFeedTab} uploadedImage={uploadedImage} uploadedFile={uploadedFile} uploadedFileName={uploadedFileName} uploadError={uploadError} selectedSample={selectedSample} fileInputRef={fileInputRef} handleFileUpload={handleFileUpload} handleSampleSelect={handleSampleSelect}/>;
}

function LiveMonitoringDashboard({stationId,setPage,mode,setMode,camera,busy,result,message,candidate,history,videoRef,canvasRef,detectedBbox,liveScanStatus,startCamera,stopCamera,feedTab,setFeedTab,uploadedImage,uploadedFile,uploadedFileName,uploadError,selectedSample,fileInputRef,handleFileUpload,handleSampleSelect}){
 const [journeys,setJourneys]=useState([]),[backendState,setBackendState]=useState('Checking'),[selectedCamera,setSelectedCamera]=useState('Entry Lane 1'),[now,setNow]=useState(()=>new Date());
 const cameras=['Entry Lane 1','Entry Lane 2','Bay Camera 1','Bay Camera 2','Exit Lane'];
 useEffect(()=>{
  let active=true;
  const load=async()=>{
   try{
    const list=await journeyApi.list(stationId);
    if(!active)return;
    setJourneys(Array.isArray(list)?list:Array.isArray(list?.content)?list.content:[]);
    setBackendState('Connected');
   }catch{if(active)setBackendState('Unavailable');}
  };
  load();
  const poll=setInterval(load,3000),clock=setInterval(()=>setNow(new Date()),1000);
  return()=>{active=false;clearInterval(poll);clearInterval(clock);};
 },[stationId,result]);
 const activeJourneys=journeys.filter(j=>!['EXITED','BLOCKED'].includes(String(j.status||'').toUpperCase()));
 const activity=journeys.slice(0,6);
 const time=now.toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false,timeZone:'Asia/Kolkata'});
 const chooseCamera=name=>{setSelectedCamera(name);setMode(name==='Exit Lane'?'EXIT':'ENTRY')};
 const displayedFlow=history.slice(0,5);
 const showVideo=camera==='live'&&feedTab==='CAMERA';

 return <div className="live-monitor-dashboard">
  {/* Hidden high-performance capture canvas */}
  <canvas ref={canvasRef} style={{display:'none'}} aria-hidden="true"/>

  <section className="monitor-heading">
   <div>
    <span className="monitor-eyebrow">LIVE MONITORING & ANPR TESTING</span>
    <h1>Station Live Monitoring & Plate Detection</h1>
    <p>Upload Indian license plate photos or use the camera to detect plates and add vehicles to the fueling queue.</p>
   </div>
   <div className="monitor-health">
    <div className="health-card">
     <span className="health-icon green">◉</span>
     <span><b>{feedTab==='UPLOAD'?'UPLOAD MODE':camera==='live'?'CAM 01 LIVE':'STANDBY'}</b><small>{feedTab==='UPLOAD'?'File input ready':camera==='live'?'Live stream active':'Standby'}</small></span>
    </div>
    <div className="health-card">
     <span className="health-icon teal">▤</span>
     <span><b>Backend</b><small className={backendState==='Connected'?'health-good':'health-off'}>{backendState}</small></span>
    </div>
    <div className="health-card">
     <span className="health-icon teal">⌁</span>
     <span><b>Fueling Queue</b><small className="health-good">{activeJourneys.length} Active Vehicles</small></span>
    </div>
   </div>
  </section>

  <section className="monitor-camera-tabs" aria-label="Select camera view">
   {cameras.map((name,i)=><button key={name} className={selectedCamera===name?'selected':''} onClick={()=>chooseCamera(name)}><span className="camera-tab-icon">{i===4?'⇥':i>1?'▣':'▰'}</span>{name}{selectedCamera===name&&camera==='live'&&feedTab==='CAMERA'&&<i className="tab-live">LIVE</i>}</button>)}
   <span className="monitor-mode">{mode} MODE</span>
  </section>

  <section className="monitor-top-grid">
   <article className="monitor-panel monitor-feed-panel">
    <div className="feed-source-tabs">
     <button type="button" className={`feed-source-tab ${feedTab==='UPLOAD'?'active':''}`} onClick={()=>{setFeedTab('UPLOAD');if(camera==='live')stopCamera();}}>
      📁 Upload Plate Photo / Test Image
     </button>
     <button type="button" className={`feed-source-tab ${feedTab==='CAMERA'?'active':''}`} onClick={()=>setFeedTab('CAMERA')}>
      📹 Live Camera Feed
     </button>
    </div>

    {feedTab==='UPLOAD' ? (
     <>
      <div className="monitor-feed">
       {uploadedImage ? (
        <div className="upload-preview-container" onDragOver={e=>{e.preventDefault();e.stopPropagation();}} onDrop={e=>{e.preventDefault();e.stopPropagation();if(e.dataTransfer?.files?.[0])handleFileUpload(e.dataTransfer.files[0]);}}>
         <img className="upload-preview-image" src={uploadedImage} alt="Uploaded vehicle"/>
         {busy && (
          <>
           <div className="scanning-laser"/>
           <div className="scanning-indicator-badge">
            <span className="spinner" style={{width:12,height:12}}/>
            <span>AI DETECTING NUMBER PLATE & OCR...</span>
           </div>
          </>
         )}
         {candidate && (
          <div className="monitor-read-plate">
           <small>PLATE READ · {Math.round((result?.confidence||0.9)*100)}% CONFIDENCE</small>
           <Plate plate={candidate}/>
          </div>
         )}
         {uploadError && !busy && (
          <div className="feed-demo-note" style={{background:'#7f1d1d',borderColor:'#ef4444'}}>
           {uploadError}
          </div>
         )}
        </div>
       ) : (
        <div className="upload-dropzone" onClick={()=>fileInputRef.current?.click()} onDragOver={e=>{e.preventDefault();e.stopPropagation();}} onDrop={e=>{e.preventDefault();e.stopPropagation();if(e.dataTransfer?.files?.[0])handleFileUpload(e.dataTransfer.files[0]);}}>
         <div className="upload-icon">📁</div>
         <h3>Upload Vehicle Number Plate Photo</h3>
         <p>Drag and drop any photo of an Indian vehicle or license plate here, or click to browse from device.</p>
         <button type="button" className="btn-primary" style={{pointerEvents:'none'}}>Select Photo from Device</button>
        </div>
       )}
       <input type="file" ref={fileInputRef} accept="image/*" style={{display:'none'}} onChange={e=>{if(e.target.files?.[0]){handleFileUpload(e.target.files[0]);e.target.value='';}}}/>
      </div>

      <div className="monitor-action-row">
       <button type="button" className="btn-primary" onClick={()=>fileInputRef.current?.click()} disabled={busy}>
        📁 {uploadedImage?'Choose Different Photo':'Browse Photo'}
       </button>
       {uploadedFile && (
        <button type="button" className="btn-outline" disabled={busy} onClick={()=>handleFileUpload(uploadedFile)}>
         ↺ Re-analyze Image
        </button>
       )}
       <span style={{fontSize:'12px',color:'#64748b',marginLeft:'auto'}}>
        {busy?'Processing with YOLO & OCR...':uploadedFileName?`File: ${uploadedFileName}`:'Accepts JPG, PNG, WEBP'}
       </span>
      </div>

      <div className="sample-presets-bar">
       <span className="sample-presets-label">⚡ Quick Test Samples:</span>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/rj_kia.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/rj_kia.jpg','RJ14CV0002')}>
        🚗 RJ14CV0002 (Rajasthan Kia)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/hr_hsrp.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/hr_hsrp.jpg','HR98AA0000')}>
        🚗 HR98AA0000 (Haryana HSRP)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/gj_car.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/gj_car.jpg','GJ01AB1234')}>
        🚗 GJ01AB1234 (GJ Car)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/dl_car.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/dl_car.jpg','DL2CCE9999')}>
        🚗 DL2CCE9999 (DL Car)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/ka_car.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/ka_car.jpg','KA05MN4321')}>
        🚗 KA05MN4321 (KA Car)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/mh_car.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/mh_car.jpg','MH12DE5678')}>
        🚗 MH12DE5678 (MH Car)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/up_auto.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/up_auto.jpg','UP32BZ1122')}>
        🛺 UP32BZ1122 (Auto)
       </button>
       <button type="button" className={`sample-preset-btn ${selectedSample==='/samples/tn_car.jpg'?'active':''}`} onClick={()=>handleSampleSelect('/samples/tn_car.jpg','TN09XY8765')}>
        🚗 TN09XY8765 (TN Car)
       </button>
      </div>
     </>
    ) : (
     <>
      <div className="monitor-feed">
       <img className="monitor-demo-image" src="/images/cng-station-login.jpg" alt="Demonstration CNG station camera frame"/>
       <video className="monitor-video" autoPlay playsInline muted ref={videoRef} style={{display:showVideo?'block':'none'}}/>

       {/* Green laser scanning effect & HUD active while camera is running */}
       {camera==='live' && (
        <>
         <div className="scanning-laser"/>
         <div className="scanning-indicator-badge">
          <span className="live-dot" style={{width:8,height:8,background:'#22c55e',borderRadius:'50%'}}/>
          <span>AI CAMERA SCANNING: {liveScanStatus}</span>
         </div>
         {detectedBbox && (
          <div className="live-camera-bbox-indicator" title="Detected License Plate Region">
           <span className="live-camera-bbox-tag">PLATE DETECTED</span>
          </div>
         )}
        </>
       )}

       <div className="monitor-feed-shade"/>
       <div className="monitor-feed-top">
        <span><i className={camera==='live'?'feed-dot on':'feed-dot'}/>{selectedCamera.toUpperCase()}</span>
        <b className={camera==='live'?'live-label':'demo-label'}>{camera==='live'?'LIVE INPUT':camera==='starting'?'CONNECTING':'DEMO FOOTAGE'}</b>
        <time>{time}</time>
       </div>
       {camera!=='live'&&<div className="feed-demo-note">{camera==='starting'?'CONNECTING TO CAMERA…':'DEMO FRAME · Not a live camera'}</div>}
       {candidate&&<div className="monitor-read-plate"><small>PLATE READ · {mode}</small><Plate plate={candidate}/></div>}
       <div className="monitor-feed-bottom"><span>{busy?'Processing camera frame':camera==='live'?'AI live recognition active':'Station reference image'}</span><span>{mode} MODE</span></div>
      </div>
      <div className="monitor-camera-controls">
       <div>
        <b>{camera==='live'?'Camera connected & scanning':camera==='starting'?'Waiting for camera permission':`${selectedCamera} selected`}</b>
        <small>{camera==='live'?'YOLOv8 detects plate bounding box; EasyOCR reads characters in real time.':camera==='starting'?'Approve camera prompt in browser.':'Start camera to run live plate detection.'}</small>
       </div>
       <button className={camera==='live'?'monitor-stop-button':'monitor-start-button'} disabled={camera==='starting'} onClick={camera==='live'?stopCamera:startCamera}>
        {camera==='live'?'Stop camera':camera==='starting'?'Connecting…':'Start camera'} <span>{camera==='live'?'■':camera==='starting'?'…':'→'}</span>
       </button>
      </div>
     </>
    )}
   </article>

   <article className="monitor-panel detected-panel">
    <div className="monitor-panel-title">
     <div><h2>Detected vehicle</h2><p>{result?'Latest ANPR detection':'Waiting for vehicle detection'}</p></div>
     <time>{result?'Just now':'—'}</time>
    </div>
    {result ? (
     <>
      <div className="detected-plate-row">
       <strong>{result.plate}</strong>
       <span className={'confidence-chip '+(Number(result.confidence)>=.8?'':'medium')}>✓ {Math.round(Number(result.confidence||0)*100)}%</span>
      </div>
      <div className="detected-checks">
       <div><span>Journey status</span><b>{String(result.journeyStatus||'—').replaceAll('_',' ')}</b></div>
       <div><span>Compliance</span><b className={String(result.status).toUpperCase()==='ELIGIBLE'?'check-good':'check-review'}>{String(result.status||'Awaiting result').replaceAll('_',' ')}</b></div>
       <div><span>Journey ID</span><b>{result.journeyId?`#${result.journeyId}`:'Not created'}</b></div>
      </div>
      {result.duplicate&&<p className="monitor-note">This vehicle already has an active journey; no duplicate was added.</p>}
      <button className={'monitor-result-cta '+(String(result.status).toUpperCase()==='ELIGIBLE'?'cleared':'blocked')} onClick={()=>setPage?.('Bays & Queue')}>
       {String(result.status).toUpperCase()==='ELIGIBLE'?'View in Fueling Queue →':'Review station record →'}
      </button>
     </>
    ) : (
     <div className="detected-empty">
      <div className="empty-car-icon">▰</div>
      <p>Vehicle plate reads and fueling queue status will appear here.</p>
      <small>Upload an Indian license plate photo or start the live camera to test.</small>
     </div>
    )}
   </article>

   <article className="monitor-panel detection-flow-panel">
    <div className="monitor-panel-title">
     <div><h2>Detection flow</h2><p>Recent ANPR detections</p></div>
     <span className={'flow-state '+(feedTab==='UPLOAD'||camera==='live'?'on':'')}>{feedTab==='UPLOAD'?'● Ready to Upload':camera==='live'?'● Camera Active':'○ Standby'}</span>
    </div>
    {displayedFlow.length ? (
     <div className="detection-flow-list">
      {displayedFlow.map((item,index)=><div className="flow-event" key={`${item.plate}-${item.time}-${index}`}><i className="flow-node"/><span className="flow-event-icon">{index===0?'◉':'▤'}</span><span><b>{item.mode} · {item.plate}</b><small>{String(item.journeyStatus||item.status||'Detection recorded').replaceAll('_',' ')}</small></span><time>{item.time}</time></div>)}
     </div>
    ) : (
     <div className="flow-empty">
      <span>◎</span>
      <b>No detections yet</b>
      <small>Results will appear here after an image is uploaded or a live plate is recognized.</small>
     </div>
    )}
   </article>
  </section>

  <section className="monitor-bottom-grid">
   <article className="monitor-panel active-vehicles-panel">
    <div className="monitor-panel-title">
     <div><h2>Active vehicles in queue & station <small>({activeJourneys.length})</small></h2><p>Current journey status</p></div>
     <button onClick={()=>setPage?.('Bays & Queue')}>View all <span>→</span></button>
    </div>
    <div className="monitor-table-wrap">
     <table className="monitor-table">
      <thead><tr><th>#</th><th>Vehicle number</th><th>Current stage</th><th>Duration</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>
       {activeJourneys.slice(0,7).map((j,i)=><tr key={j.id}><td><span className="vehicle-rank">{i+1}</span></td><td><b>{j.registrationNumber||'UNKNOWN'}</b></td><td>{String(j.status||'—').replaceAll('_',' ')}</td><td>{j.entryTime?`${Math.max(0,Math.round((Date.now()-new Date(j.entryTime).getTime())/60000))} min`:'—'}</td><td><span className={'journey-pill '+(String(j.complianceStatus).toUpperCase()==='ELIGIBLE'?'eligible':'review')}>{String(j.complianceStatus||j.status||'UNKNOWN').replaceAll('_',' ')}</span></td><td><button onClick={()=>setPage?.('Bays & Queue')}>View in Queue</button></td></tr>)}
      </tbody>
     </table>
     {!activeJourneys.length&&<div className="monitor-empty-row">No active journeys. Detected vehicles will appear here automatically.</div>}
    </div>
   </article>

   <article className="monitor-panel station-activity-panel">
    <div className="monitor-panel-title"><div><h2>Station activity</h2><p>Latest recorded journeys</p></div><button onClick={()=>setPage?.('Audit Logs')}>View all <span>→</span></button></div>
    {activity.length?<div className="station-activity-list">{activity.map(j=><div key={j.id}><i className={String(j.complianceStatus).toUpperCase()==='ELIGIBLE'?'activity-good':'activity-info'}/><span><b>{j.registrationNumber||'Vehicle'} · {String(j.status||'Journey update').replaceAll('_',' ')}</b><small>{String(j.complianceStatus||'Status not reported').replaceAll('_',' ')}</small></span><time>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}):'—'}</time></div>)}</div>:<div className="monitor-empty-copy">Station events appear here when journeys are recorded.</div>}
   </article>

   <article className="monitor-panel camera-status-panel">
    <div className="monitor-panel-title"><div><h2>Camera status</h2><p>Camera preview images</p></div><span className="flow-state">DEMO FRAMES</span></div>
    <div className="camera-preview-grid">{cameras.slice(0,4).map((name,i)=><button key={name} onClick={()=>chooseCamera(name)} className={selectedCamera===name?'chosen':''}><span className="camera-preview-image"><img src="/images/cng-station-login.jpg" alt=""/><i className={camera==='live'&&selectedCamera===name?'preview-live-dot':'preview-demo-dot'}/><b>DEMO</b></span><span className="camera-preview-name">{name}</span></button>)}</div>
    <small className="camera-status-caption">Upload images above or use the laptop camera in entry/exit modes.</small>
   </article>
  </section>

  {message&&<div className="monitor-service-message" role="status">{message}</div>}
 </div>;
}

function JourneyQueue({stationId}){const [journeys,setJourneys]=useState([]),[error,setError]=useState(''),[busy,setBusy]=useState(null),[bayIds,setBayIds]=useState({});async function refresh(){try{const list=await journeyApi.list(stationId);setJourneys(Array.isArray(list)?list:[]);setError('')}catch(e){setError(e.message)}}useEffect(()=>{refresh()},[stationId]);useEffect(()=>{const timer=setInterval(refresh,2000);return()=>clearInterval(timer)},[stationId]);async function advance(j){setBusy(j.id);setError('');try{switch(j.status){case'ENTERED':await journeyApi.enterQueue(j.id);break;case'IN_QUEUE':{const raw=bayIds[j.id];if(!raw||!Number(raw))throw new Error('Enter the numeric fueling bay ID supplied by station setup.');await journeyApi.assignBay(j.id,Number(raw));break}case'BAY_ASSIGNED':await journeyApi.startFueling(j.id);break;case'FUELING':await journeyApi.completeFueling(j.id);break;case'FUELING_COMPLETED':await journeyApi.exit(j.id);break;default:return}await refresh()}catch(e){setError(e.message)}finally{setBusy(null)}}const action={ENTERED:'Add to queue',IN_QUEUE:'Assign bay',BAY_ASSIGNED:'Start fueling',FUELING:'Complete fueling',FUELING_COMPLETED:'Record exit'};return <><PageTitle eyebrow="VEHICLE JOURNEY" title="Journey queue" sub="Move eligible vehicles through queue, fueling and station exit." action={<button className="btn-outline" onClick={refresh}>↻ &nbsp;Refresh</button>}/>{error&&<div className="service-notice" role="alert">{error}</div>}<div className="panel table-panel"><div className="table-toolbar"><div><h3>Active station journeys</h3><p>Fueling actions are enabled only for eligible vehicles.</p></div><span className="muted">{journeys.filter(j=>j.status!=='EXITED').length} active</span></div><table><thead><tr><th>VEHICLE</th><th>ARRIVED</th><th>COMPLIANCE</th><th>JOURNEY STATUS</th><th>FUELING BAY</th><th>ACTION</th></tr></thead><tbody>{journeys.filter(j=>j.status!=='EXITED').map(j=>{const eligible=String(j.complianceStatus)==='ELIGIBLE';return <tr key={j.id}><td><Plate plate={j.registrationNumber||'UNKNOWN'}/></td><td>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB'):'—'}</td><td><Badge tone={eligible?'pass':'fail'}>{String(j.complianceStatus||'UNKNOWN').replaceAll('_',' ')}</Badge></td><td>{String(j.status||'—').replaceAll('_',' ')}</td><td>{j.assignedBayNumber?`Bay ${j.assignedBayNumber}`:j.status==='IN_QUEUE'&&eligible?<input className="bay-id-input" aria-label={`Fueling bay ID for ${j.registrationNumber}`} type="number" min="1" placeholder="Bay ID" value={bayIds[j.id]||''} onChange={e=>setBayIds({...bayIds,[j.id]:e.target.value})}/>:<span>—</span>}</td><td>{action[j.status]&&eligible?<button className="btn-outline" disabled={busy===j.id} onClick={()=>advance(j)}>{busy===j.id?'Saving…':action[j.status]}</button>:!eligible&&j.status!=='EXITED'?<Badge tone="fail">DO NOT FUEL</Badge>:<span>—</span>}</td></tr>})}</tbody></table>{!journeys.filter(j=>j.status!=='EXITED').length&&<div className="table-foot">No active journeys. ANPR detections will appear here after they are recorded.</div>}</div><div className="compliance-note"><span>i</span><span><b>Eligibility interlock is active.</b> Non-compliant or unknown vehicles cannot enter the queue or progress to fueling.</span></div></>}

function BayOperations({stationId}){
 const [journeys,setJourneys]=useState([]),[bayRecords,setBayRecords]=useState([]),[bayCount,setBayCount]=useState(6),[choices,setChoices]=useState({}),[busy,setBusy]=useState(null),[error,setError]=useState(''),[demoPlate,setDemoPlate]=useState(demoVehicles[0].plate),[demoMessage,setDemoMessage]=useState(''),[demoBusy,setDemoBusy]=useState(false);
 const busyRef=useRef(false),refreshRef=useRef(null),refreshAgainRef=useRef(false);
 async function refresh(force=false){if(refreshRef.current){if(force)refreshAgainRef.current=true;return refreshRef.current}const pending=(async()=>{do{refreshAgainRef.current=false;try{const [items,util,bayList]=await Promise.all([journeyApi.list(stationId),dashboardApi.utilization(stationId),stationApi.bays(stationId)]);setJourneys(Array.isArray(items)?items:[]);setBayRecords(Array.isArray(bayList)?bayList:[]);setBayCount(Math.max(6,Number(util?.totalBays)||bayList?.length||6));setError('')}catch(e){setError(e.message||'Station operations are unavailable.')}}while(refreshAgainRef.current)})();refreshRef.current=pending;try{await pending}finally{refreshRef.current=null}}
 useEffect(()=>{refresh();const timer=setInterval(()=>{if(!busyRef.current)refresh()},5000);return()=>clearInterval(timer)},[stationId]);
 const active=journeys.filter(j=>j.status!=='EXITED'&&j.status!=='BLOCKED');
 const queue=active.filter(j=>['ENTERED','IN_QUEUE'].includes(j.status)).sort((a,b)=>new Date(a.queueEntryTime||a.entryTime||0)-new Date(b.queueEntryTime||b.entryTime||0));
 const assigned=active.filter(j=>['BAY_ASSIGNED','FUELING'].includes(j.status)&&j.assignedBayNumber),occupied=new Map(assigned.map(j=>[Number(j.assignedBayNumber),j]));
 const bays=bayRecords.length?bayRecords:Array.from({length:bayCount},(_,i)=>({id:i+1,bayNumber:i+1,status:'AVAILABLE'}));
 const bayNumber=b=>Number(b.bayNumber??b.number??b.id);
 const available=bays.filter(b=>['AVAILABLE','READY','ACTIVE'].includes(String(b.status||'AVAILABLE').toUpperCase())&&!occupied.has(bayNumber(b)));
 const blocked=journeys.filter(j=>j.status==='BLOCKED'||j.complianceStatus!=='ELIGIBLE'&&j.status!=='EXITED');
 async function act(j,operation){busyRef.current=true;setBusy(j.id);setError('');try{if(operation==='queue')await journeyApi.enterQueue(j.id);if(operation==='assign'){const selected=available.find(b=>String(b.id)===String(choices[j.id]));if(!selected)throw new Error('That bay is no longer available. Choose another available bay.');await journeyApi.assignBay(j.id,Number(selected.id));setChoices(old=>({...old,[j.id]:''}))}if(operation==='start')await journeyApi.startFueling(j.id);if(operation==='finish')await journeyApi.completeFueling(j.id);await refresh(true)}catch(e){setError(e.message||'The operation could not be completed.')}finally{busyRef.current=false;setBusy(null)}}
 async function simulateArrival(){setDemoBusy(true);setDemoMessage('');try{const clean=demoPlate;const known=await journeyApi.list(stationId);const duplicate=(Array.isArray(known)?known:[]).find(j=>j.registrationNumber===clean&&j.status!=='EXITED');if(duplicate){setDemoMessage(`${clean} already has an active journey (#${duplicate.id}); no duplicate was created.`);return}const created=await anprApi.ingest({stationId:Number(stationId),cameraId:Number(import.meta.env.VITE_CAMERA_ID)||1,registrationNumber:clean,detectedAt:backendLocalDateTime()});const createdId=created.journeyId??created.id;let latest=created;if(String(created.complianceStatus).toUpperCase()==='ELIGIBLE'&&createdId&&created.journeyStatus==='ENTERED')latest=await journeyApi.enterQueue(createdId);setDemoMessage(`${clean}: ${String(created.complianceStatus||'UNKNOWN').replaceAll('_',' ')} · ${String(latest.journeyStatus||latest.status||created.journeyStatus||created.status||'RECORDED').replaceAll('_',' ')}${created.message?` · ${created.message}`:''}`);await refresh(true)}catch(e){setDemoMessage(e.message||'Sample vehicle could not be recorded.')}finally{setDemoBusy(false)}}
 const elapsed=j=>`${Math.max(0,Math.floor((Date.now()-new Date(j.queueEntryTime||j.entryTime||Date.now()).getTime())/60000))} min`;
 return <div className="bay-console"><PageTitle eyebrow="STATION OPERATIONS" title="Bays & Queue" sub="Manage fueling bays and keep vehicles moving through the station." action={<button className="btn-outline" onClick={()=>refresh(true)}>↻ &nbsp;Refresh</button>}/>{error&&<div className="service-notice" role="alert">{error}</div>}
 <div className="bay-kpis"><div><i className="blue">▣</i><span><b>{journeys.length}</b><small>Total vehicles</small></span></div><div><i className="amber">◉</i><span><b>{queue.length}</b><small>In queue</small></span></div><div><i className="red">▰</i><span><b>{assigned.filter(j=>j.status==='FUELING').length}</b><small>Fueling now</small></span></div><div><i className="green">⛽</i><span><b>{available.length}/{bays.length}</b><small>Bays available</small></span></div></div>
 <div className="bay-console-grid"><div className="bay-console-main">
 <section className="bay-dark-panel"><div className="bay-panel-heading"><h2>Fueling Bay Status</h2><div className="bay-legend"><span><i className="green-dot"/>Available</span><span><i className="blue-dot"/>Ready</span><span><i className="amber-dot"/>Fueling</span><span><i className="red-dot"/>Blocked</span><span><i className="gray-dot"/>Offline</span></div></div><div className="bay-grid">{bays.map(b=>{const n=bayNumber(b),j=occupied.get(n),isFueling=j?.status==='FUELING',status=String(b.status||'AVAILABLE').toUpperCase(),offline=['OFFLINE','MAINTENANCE','BLOCKED'].includes(status);return <article className={`bay-card ${offline?'is-offline':j?isFueling?'is-fueling':'is-reserved':'is-available'}`} key={b.id}><div className="bay-card-top"><span className="bay-number">BAY {String(n).padStart(2,'0')}</span><Badge tone={offline?'fail':isFueling?'warn':j?'neutral':'pass'}>{offline?status:isFueling?'FUELING':j?'READY':'AVAILABLE'}</Badge></div>{j?<><div className="bay-vehicle"><Plate plate={j.registrationNumber||'UNKNOWN'}/><small>{isFueling?'Fueling in progress':'Assigned · ready to start'}</small></div><div className="bay-card-bottom">{isFueling?<button className="btn-primary" disabled={busy===j.id} onClick={()=>act(j,'finish')}>{busy===j.id?'Saving…':'Finish fueling'}</button>:<button className="btn-primary" disabled={busy===j.id} onClick={()=>act(j,'start')}>{busy===j.id?'Starting…':'Start fueling'}</button>}</div></>:offline?<div className="bay-available-copy"><span>Unavailable</span><small>{b.note||'Station maintenance'}</small></div>:<div className="bay-available-copy"><span>Ready for next vehicle</span><small>{queue.length?`${queue.length} vehicle${queue.length===1?'':'s'} waiting`: 'No vehicles waiting'}</small></div>}</article>})}</div></section>
 <section className="bay-dark-panel active-fueling-panel"><div className="bay-panel-heading"><h2>Active Fueling <small>({assigned.length} vehicles)</small></h2><span className="bay-muted">Manual start and finish controls</span></div><div className="bay-table-scroll"><table className="bay-dark-table"><thead><tr><th>Bay</th><th>Vehicle Number</th><th>Fuel Type</th><th>Start Time</th><th>Status</th><th>Action</th></tr></thead><tbody>{assigned.map(j=><tr key={j.id}><td>{String(j.assignedBayNumber).padStart(2,'0')}</td><td><Plate plate={j.registrationNumber}/></td><td>{j.fuelType||'CNG'}</td><td>{j.fuelingStartTime?new Date(j.fuelingStartTime).toLocaleTimeString('en-GB'):'—'}</td><td><Badge tone={j.status==='FUELING'?'warn':'neutral'}>{j.status==='FUELING'?'FUELING':'READY'}</Badge></td><td>{j.status==='FUELING'?<button className="btn-primary" disabled={busy===j.id} onClick={()=>act(j,'finish')}>{busy===j.id?'Saving…':'Finish'}</button>:<button className="btn-light" disabled={busy===j.id} onClick={()=>act(j,'start')}>{busy===j.id?'Starting…':'Start'}</button>}</td></tr>)}</tbody></table>{!assigned.length&&<div className="bay-empty"><b>No active fueling</b><span>Assigned vehicles will appear here.</span></div>}</div></section>
 </div><aside className="bay-console-side"><section className="bay-dark-panel"><div className="bay-panel-heading"><h2>Queue Management <small>({queue.length} vehicles)</small></h2></div><div className="bay-table-scroll"><table className="bay-dark-table"><thead><tr><th>#</th><th>Vehicle Number</th><th>Waiting</th><th>Assignment</th></tr></thead><tbody>{queue.map((j,i)=><tr key={j.id}><td>{i+1}</td><td><div className="bay-queue-vehicle"><Plate plate={j.registrationNumber||'UNKNOWN'}/><Badge tone={j.complianceStatus==='ELIGIBLE'?'pass':'fail'}>{j.complianceStatus||'UNKNOWN'}</Badge></div></td><td>{elapsed(j)}</td><td>{j.status==='ENTERED'?<button className="btn-light" disabled={busy===j.id||j.complianceStatus!=='ELIGIBLE'} onClick={()=>act(j,'queue')}>{busy===j.id?'Saving…':'Next'}</button>:<div className="bay-assign-controls"><select aria-label={`Select a bay for ${j.registrationNumber}`} value={choices[j.id]||''} onChange={e=>setChoices({...choices,[j.id]:e.target.value})}><option value="">Bay</option>{available.map(b=><option value={b.id} key={b.id}>{String(bayNumber(b)).padStart(2,'0')}</option>)}</select><button className="btn-light" disabled={busy===j.id||!choices[j.id]} onClick={()=>act(j,'assign')}>{busy===j.id?'…':'Assign'}</button></div>}</td></tr>)}</tbody></table>{!queue.length&&<div className="bay-empty"><b>Queue is clear</b><span>Entry detections will appear here automatically.</span></div>}</div></section>
 <section className="bay-dark-panel"><div className="bay-panel-heading"><h2>Queue Insights</h2></div><div className="queue-insight-grid"><div><b>{queue.length}</b><span>Vehicles waiting</span></div><div><b>{blocked.length}</b><span>Blocked today</span></div><div><b>{assigned.length}</b><span>Assigned bays</span></div><div><b>{available.length}</b><span>Available bays</span></div></div></section><section className="bay-dark-panel"><div className="bay-panel-heading"><h2>Recent Station Activity</h2></div><div className="bay-recent-activity">{journeys.slice(0,5).map(j=><div key={j.id}><i className={j.status==='BLOCKED'?'red-dot':'green-dot'}/><span>{j.registrationNumber} · {String(j.status||'').replaceAll('_',' ')}</span><small>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB'):'—'}</small></div>)}</div></section></aside></div>
 <div className="bay-footnotes"><span><b>Exit gate:</b> exit detections complete fueling and close the station journey automatically.</span>{blocked.length>0&&<span><b>Blocked:</b> {blocked.length} vehicle{blocked.length===1?'':'s'} held outside the fueling queue.</span>}</div><details className="demo-vehicle-book"><summary>Demo vehicle book <span>36 mock hydro-test records · 24 valid · 12 expired</span></summary><div className="demo-book-controls"><div><label htmlFor="demo-registration">SAMPLE REGISTRATION</label><select id="demo-registration" value={demoPlate} onChange={e=>setDemoPlate(e.target.value)}>{demoVehicles.map(v=><option key={v.plate} value={v.plate}>{v.plate} · Hydro-test {v.hydro}</option>)}</select></div><button className="btn-outline" disabled={demoBusy} onClick={simulateArrival}>{demoBusy?'Recording…':'Simulate entry detection'}</button></div><p>Uses the station’s existing mock registry and hydro-test service, then creates a real journey through the existing ANPR endpoint.</p>{demoMessage&&<div className="service-notice" role="status">{demoMessage}</div>}</details></div>}

function Overview({setPage,stationId}){const [data,setData]=useState(null),[queueStats,setQueueStats]=useState(null),[throughput,setThroughput]=useState(null),[utilization,setUtilization]=useState(null),[compliance,setCompliance]=useState(null),[journeys,setJourneys]=useState([]),[alerts,setAlerts]=useState([]),[bayRecords,setBayRecords]=useState([]),[loadError,setLoadError]=useState('');useEffect(()=>{let live=true;const load=async()=>{const results=await Promise.allSettled([dashboardApi.get(stationId),journeyApi.list(stationId),dashboardApi.queueMetrics(stationId),dashboardApi.throughput(stationId),dashboardApi.utilization(stationId),dashboardApi.metrics(stationId),alertApi.list(stationId),stationApi.bays(stationId)]);if(!live)return;const value=i=>results[i].status==='fulfilled'?results[i].value:null;const list=v=>Array.isArray(v)?v:Array.isArray(v?.content)?v.content:[];if(value(0))setData(value(0));if(value(1))setJourneys(list(value(1)));if(value(2))setQueueStats(value(2));if(value(3))setThroughput(value(3));if(value(4))setUtilization(value(4));if(value(5))setCompliance(value(5));if(value(6))setAlerts(list(value(6)));if(value(7))setBayRecords(list(value(7)));const firstFailure=results.find(r=>r.status==='rejected');setLoadError(firstFailure?firstFailure.reason?.message||'Some station information is unavailable.':'')};load();const timer=setInterval(load,15000);return()=>{live=false;clearInterval(timer)}},[stationId]);const name=stationsName(data),active=journeys.filter(j=>!['EXITED','BLOCKED'].includes(String(j.status||'').toUpperCase())),queue=active.filter(j=>['ENTERED','IN_QUEUE'].includes(String(j.status||'').toUpperCase())),fueling=active.filter(j=>String(j.status||'').toUpperCase()==='FUELING'),recent=journeys.slice(0,5),today=new Date().toDateString(),todayJourneys=journeys.filter(j=>{const stamp=j.entryTime||j.detectedAt||j.createdAt;return stamp&&new Date(stamp).toDateString()===today}),todayVerified=todayJourneys.filter(j=>['ELIGIBLE','BLOCKED','NOT_ELIGIBLE'].includes(String(j.complianceStatus||'').toUpperCase())).length,completed=throughput?.completedJourneysToday??data?.completedJourneysToday??'—',processed=compliance?.totalVehiclesProcessedToday??data?.totalVehiclesProcessedToday,eligible=compliance?.eligibleVehicles,complianceRate=Number(processed)>0&&Number.isFinite(Number(eligible))?Math.round(Number(eligible)/Number(processed)*100):data?.complianceRate??'—',blocked=compliance?.blockedVehicles??data?.blockedVehiclesToday??'—',totalBays=Number(utilization?.totalBays||bayRecords.length||6),available=data?.availableBays??utilization?.availableBays??Math.max(0,totalBays-active.filter(j=>j.assignedBayNumber).length),latest=journeys[0];const queueCount=data?.vehiclesInQueue??queueStats?.currentQueueLength??queue.length,fuelingCount=data?.vehiclesFueling??fueling.length;const stages=[['ENTRY',todayJourneys.length,'vehicles','green'],['VERIFICATION',todayVerified,'checked','blue'],['QUEUE',queueCount,'vehicles','amber'],['FUELING',fuelingCount,'vehicles','red'],['EXIT',completed,'completed','slate']];const bayFor=n=>active.find(j=>Number(j.assignedBayNumber)===n);const bayMeta=n=>bayRecords.find(b=>Number(b.bayNumber??b.number??b.id)===n);const toneForAlert=a=>['HIGH','CRITICAL'].includes(String(a.severity||'').toUpperCase())?'high':'medium',openAlerts=alerts.filter(a=>!['RESOLVED','DISMISSED'].includes(String(a.status||'OPEN').toUpperCase()));return <div className="overview-dashboard"><section className="overview-hero"><div className="overview-hero-copy"><span className="overview-kicker">STATION OPERATIONS</span><h1>{name}</h1><p>Real-time vehicle compliance and station operations<br className="wide-only"/> for a cleaner, safer and more efficient tomorrow.</p><div className="hero-statuses"><span><i className="status-check">✓</i>{data?'Backend connected':'Station service'}</span><span><i className="status-check">✓</i>{totalBays} station bays</span><span><i className="status-check">✓</i>Compliance monitoring</span></div></div><div className="hero-photo-shade"/><div className="hero-photo-label"><span className="weather-glyph">✳</span><b>{data?'Station operations':'Station overview'}</b><small>Ahmedabad, Gujarat</small></div></section><section className="journey-flow" aria-label="Vehicle journey stages">{stages.map(([label,value,caption,tone],i)=><React.Fragment key={label}><div className={'journey-stage '+tone}><span className="stage-icon">{['⌂','▤','⠿','⛽','⇥'][i]}</span><div><small>{label}</small><strong>{value}</strong><span>{caption}</span></div></div>{i<stages.length-1&&<span className="stage-arrow">→</span>}</React.Fragment>)}</section><section className="overview-stat-grid"><button className="overview-stat green" onClick={()=>setPage('Bays & Queue')}><span className="stat-icon">⛽</span><span><small>Bays Available</small><strong>{available}<em> / {totalBays}</em></strong></span><span className="stat-meter"><i style={{width:(Math.min(100,Math.max(0,Number(available)/Math.max(1,totalBays)*100))+'%')}}/></span><span className="stat-chevron">›</span></button><div className="overview-stat blue"><span className="stat-icon">▣</span><span><small>Total Vehicles Today</small><strong>{processed??completed}</strong></span><small className="stat-note">Station records</small></div><div className="overview-stat mint"><span className="stat-icon">✓</span><span><small>Compliance Rate</small><strong>{complianceRate}{complianceRate!=='—'?'%':''}</strong></span><small className="stat-note">Verified records</small></div><div className="overview-stat coral"><span className="stat-icon">!</span><span><small>Blocked Today</small><strong>{blocked}</strong></span><small className="stat-note">Not eligible</small></div></section>{loadError&&<div className="overview-data-note" role="status">{loadError}</div>}<section className="overview-primary-grid"><article className="overview-card camera-card"><div className="overview-card-heading"><div><h2>Live Station Feed</h2><p>Station camera view</p></div><button onClick={()=>setPage('Live Monitoring')}>Open camera <span>→</span></button></div><button className="overview-camera-image" onClick={()=>setPage('Live Monitoring')} aria-label="Open live monitoring"><span className="camera-feed-tag"><i/> STATION CAMERA · REFERENCE FRAME</span><span className="camera-feed-time">VIEW LIVE MONITORING ↗</span><span className="camera-frame-corners"/><span className="camera-anpr-tag">Station overview image · not a live feed</span></button></article><article className="overview-card vehicle-status-card"><div className="overview-card-heading"><div><h2>Latest vehicle</h2><p>{latest?'Most recent station journey':'Awaiting a station journey'}</p></div><span className={'overview-live-pill '+(latest?'active':'')}><i/>{latest?'RECORDED':'—'}</span></div>{latest?<><div className="latest-vehicle-plate"><span>{latest.registrationNumber||'UNKNOWN'}</span><Badge tone={String(latest.complianceStatus).toUpperCase()==='ELIGIBLE'?'pass':'fail'}>{String(latest.complianceStatus||'UNKNOWN').replaceAll('_',' ')}</Badge></div><div className="vehicle-check-list"><div><span>Journey status</span><b>{String(latest.status||'—').replaceAll('_',' ')}</b></div><div><span>Compliance decision</span><b className={String(latest.complianceStatus).toUpperCase()==='ELIGIBLE'?'ok':''}>{String(latest.complianceStatus||'—').replaceAll('_',' ')}</b></div><div><span>Assigned bay</span><b>{latest.assignedBayNumber?`Bay ${String(latest.assignedBayNumber).padStart(2,'0')}`:'Not assigned'}</b></div></div><button className="overview-action-button" onClick={()=>setPage('Vehicle Verification')}>Verify a vehicle <span>→</span></button></>:<div className="overview-empty">New detected vehicles and compliance outcomes will appear here.</div>}</article><article className="overview-card bay-board-card"><div className="overview-card-heading"><div><h2>Fueling bays</h2><p>{Number(totalBays)||6} bays at this station</p></div><button onClick={()=>setPage('Bays & Queue')}>View all <span>→</span></button></div><div className="overview-bay-grid">{Array.from({length:6},(_,i)=>{const n=i+1,vehicle=bayFor(n),meta=bayMeta(n),status=vehicle?String(vehicle.status).toUpperCase():String(meta?.status||'AVAILABLE').toUpperCase(),isFueling=status==='FUELING',availableBay=!vehicle&&['AVAILABLE','IDLE','FREE'].includes(status);return <button key={n} className={'overview-bay-tile '+(availableBay?'available':isFueling?'fueling':'occupied')} onClick={()=>setPage('Bays & Queue')}><span className="bay-state-dot"/><b>Bay {String(n).padStart(2,'0')}</b><span className="bay-vehicle-icon">{vehicle?'🚘':'›'}</span><strong>{vehicle?.registrationNumber|| (availableBay?'Available':String(meta?.status||status).replaceAll('_',' '))}</strong>{vehicle?<small>{isFueling?'Fueling':status==='BAY_ASSIGNED'?'Assigned':''}</small>:availableBay?<small>Ready for a vehicle</small>:<small>Check bay status</small>}</button>})}</div></article></section><section className="overview-secondary-grid"><article className="overview-card queue-card"><div className="overview-card-heading"><div><h2>Queue</h2><p>{queue.length} vehicle{queue.length===1?'':'s'} waiting</p></div><button onClick={()=>setPage('Bays & Queue')}>View all <span>→</span></button></div><div className="overview-table-wrap"><table className="overview-table"><thead><tr><th>#</th><th>Vehicle</th><th>Status</th><th>Wait</th><th>Action</th></tr></thead><tbody>{queue.slice(0,5).map((j,i)=><tr key={j.id}><td><span className="queue-rank">{i+1}</span></td><td><b>{j.registrationNumber||'UNKNOWN'}</b></td><td><span className="queue-status">{String(j.complianceStatus||'UNKNOWN').toUpperCase()==='ELIGIBLE'?'✓ Verified':'! Blocked'}</span></td><td>{j.queueEntryTime?Math.max(0,Math.round((Date.now()-new Date(j.queueEntryTime).getTime())/60000))+' min':'—'}</td><td><button onClick={()=>setPage('Bays & Queue')}>Assign bay</button></td></tr>)}</tbody></table>{!queue.length&&<div className="overview-empty">No vehicles are waiting for a bay.</div>}</div></article><article className="overview-card activity-card"><div className="overview-card-heading"><div><h2>Recent station activity</h2><p>Latest journey updates</p></div><button onClick={()=>setPage('Audit Logs')}>View all <span>→</span></button></div><div className="overview-activity-list">{recent.slice(0,5).map((j,i)=><div className="overview-activity-row" key={j.id}><i className={'activity-dot '+(String(j.complianceStatus).toUpperCase()==='ELIGIBLE'?'good':'bad')}/><span><b>{j.registrationNumber||'Vehicle'} · {String(j.status||'Updated').replaceAll('_',' ')}</b><small>{String(j.complianceStatus||'UNKNOWN').replaceAll('_',' ')}</small></span><time>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}):'—'}</time></div>)}{!recent.length&&<div className="overview-empty">Station events will be listed here as vehicles arrive.</div>}</div></article><article className="overview-card alerts-card"><div className="overview-card-heading"><div><h2>Active alerts</h2><p>{openAlerts.length} open item{openAlerts.length===1?'':'s'}</p></div><button onClick={()=>setPage('Alerts')}>View all <span>→</span></button></div><div className="overview-alert-list">{openAlerts.slice(0,4).map(a=><button className={'overview-alert '+toneForAlert(a)} key={a.id} onClick={()=>setPage('Alerts')}><span className="alert-mark">!</span><span><b>{String(a.type||'Station alert').replaceAll('_',' ')}</b><small>{a.message||a.registrationNumber||'Review alert details'}</small></span><time>{a.createdAt?new Date(a.createdAt).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}):''}</time></button>)}{!openAlerts.length&&<div className="overview-empty">No open station alerts.</div>}</div></article></section></div>}

function stationsName(data){return data?.stationName||'Station operations'}
function VerifyPage({stationId}){const [query,setQuery]=useState(''),[result,setResult]=useState(null),[loading,setLoading]=useState(false),[error,setError]=useState(''),[notFound,setNotFound]=useState(false),[journeys,setJourneys]=useState([]),[listError,setListError]=useState(''),[tab,setTab]=useState('All'),[search,setSearch]=useState(''),[fuel,setFuel]=useState('All'),[period,setPeriod]=useState('Today'),[rfidMessage,setRfidMessage]=useState(''),[uploadBusy,setUploadBusy]=useState(false);const inputRef=useRef(null),fileInputRef=useRef(null);async function loadJourneys(){try{const list=await journeyApi.list(stationId);setJourneys(Array.isArray(list)?list:[]);setListError('')}catch(e){setListError(e.message||'Journey records are unavailable.')}}useEffect(()=>{loadJourneys()},[stationId]);async function verify(e,plateOverride){e?.preventDefault();const plate=(plateOverride||query).toUpperCase().replace(/[\s-]/g,'');if(!plate)return;setQuery(plate);setError('');setNotFound(false);setLoading(true);setResult(null);try{const response=await vehicleApi.verify(plate,stationId);const vehicle=toVehicleView(response);setResult(vehicle);setNotFound(!response.vehicleFound);await loadJourneys()}catch(err){setError(err.message||'Vehicle verification failed.')}finally{setLoading(false)}}async function handleImageUpload(e){const file=e.target.files?.[0];if(!file)return;setUploadBusy(true);setError('');setRfidMessage(`Scanning ${file.name} with AI ANPR…`);try{const read=await anprApi.recognize(file);const plate=read.plateDetected&&read.registrationNumber?String(read.registrationNumber).replace(/[\s-]/g,'').toUpperCase():'';if(!plate){setError('No plate detected in this photo. Make sure the license plate is clearly visible.');setRfidMessage('');}else{setQuery(plate);setRfidMessage(`Detected ${plate} (${Math.round((read.confidence||0.9)*100)}% confidence). Verifying records…`);await verify(null,plate)}}catch(err){setError(err.message||'Failed to analyze plate image.');setRfidMessage('')}finally{setUploadBusy(false);e.target.value=''}}const isEligible=j=>String(j.complianceStatus||'').toUpperCase()==='ELIGIBLE';const isRejected=j=>['BLOCKED','REJECTED','NOT_ELIGIBLE'].includes(String(j.complianceStatus||'').toUpperCase())||['BLOCKED','REJECTED'].includes(String(j.status||'').toUpperCase());const today=new Date();const filtered=journeys.filter(j=>{const plate=String(j.registrationNumber||'').toUpperCase();const owner=String(j.ownerName||j.driverName||'').toUpperCase();const d=j.entryTime?new Date(j.entryTime):null;const within=period==='All'||!d||period==='Today'?period==='All'||!d||d.toDateString()===today.toDateString():Date.now()-d.getTime()<=7*86400000;const status=tab==='All'||(tab==='Verified'&&isEligible(j))||(tab==='Rejected'&&isRejected(j))||(tab==='Pending'&&!isEligible(j)&&!isRejected(j));const fuelMatch=fuel==='All'||String(j.fuelType||j.fuel||'CNG').toUpperCase()===fuel.toUpperCase();return status&&within&&fuelMatch&&(!search||plate.includes(search.toUpperCase())||owner.includes(search.toUpperCase()))}).sort((a,b)=>new Date(b.entryTime||0)-new Date(a.entryTime||0));const total=journeys.length,verified=journeys.filter(isEligible).length,rejected=journeys.filter(isRejected).length,pending=Math.max(0,total-verified-rejected);function exportRows(){const lines=[['Vehicle Number','Fuel Type','Driver','Entry Time','Verification Status','Journey Status'].join(','),...filtered.map(j=>[j.registrationNumber,j.fuelType||j.fuel||'CNG',j.ownerName||j.driverName||'',j.entryTime||'',isEligible(j)?'Verified':isRejected(j)?'Rejected':'Pending',j.status||''].map(v=>'"'+String(v??'').replaceAll('"','""')+'"').join(','))].join('\n');const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([lines],{type:'text/csv'}));a.download='sgl-vehicle-verification.csv';a.click();URL.revokeObjectURL(a.href)}const displayed=result||null;return <div className="verification-console"><div className="verification-heading"><div><div className="operator-eyebrow">STATION OPERATIONS</div><h1>Vehicle Verification</h1><p>Verify vehicle details, documents and compliance before fueling.</p></div><div className="verification-kpis"><div className="verification-kpi blue"><i>▣</i><span><b>{total}</b><small>Total Vehicles</small></span></div><div className="verification-kpi green"><i>✓</i><span><b>{verified}</b><small>Verified <em>{total?Math.round(verified/total*100):0}%</em></small></span></div><div className="verification-kpi amber"><i>◷</i><span><b>{pending}</b><small>Pending <em>{total?Math.round(pending/total*100):0}%</em></small></span></div><div className="verification-kpi red"><i>!</i><span><b>{rejected}</b><small>Rejected <em>{total?Math.round(rejected/total*100):0}%</em></small></span></div></div></div>{listError&&<div className="operator-notice">Journey list unavailable: {listError}</div>}<div className="verification-workspace"><div className="verification-main"><section className="verification-tools"><div className="verification-tabs">{[['All',total],['Verified',verified],['Pending',pending],['Rejected',rejected]].map(([name,count])=><button key={name} className={tab===name?'active':''} onClick={()=>setTab(name)}>{name} ({count})</button>)}</div><label className="verification-search"><span>⌕</span><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search by vehicle number or driver…"/></label><select value={fuel} onChange={e=>setFuel(e.target.value)} aria-label="Fuel type"><option>All</option><option>CNG</option><option>LNG</option></select><select value={period} onChange={e=>setPeriod(e.target.value)} aria-label="Date range"><option>Today</option><option>7 days</option><option>All</option></select><button className="verification-export" onClick={exportRows}>↓ <span>Export</span></button></section><section className="operator-panel verification-table-panel"><div className="operator-panel-title"><div><h2>Station vehicles <small>({filtered.length})</small></h2><p>Latest vehicle journeys and their compliance status</p></div><button onClick={loadJourneys}>↻ &nbsp;Refresh</button></div><div className="operator-table-scroll"><table className="operator-table"><thead><tr><th>#</th><th>Vehicle Number</th><th>Fuel Type</th><th>Driver Name</th><th>Entry Time</th><th>Verification Status</th><th>Documents</th><th>Action</th></tr></thead><tbody>{filtered.map((j,i)=>{const eligible=isEligible(j),blocked=isRejected(j);return <tr key={j.id||j.registrationNumber}><td><span className="row-number">{i+1}</span></td><td><b>{j.registrationNumber||'Unknown'}</b></td><td><span className={'fuel-chip '+(String(j.fuelType||j.fuel||'CNG').toLowerCase()==='lng'?'lng':'')}>◆ &nbsp;{j.fuelType||j.fuel||'CNG'}</span></td><td>{j.ownerName||j.driverName||'—'}</td><td>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}):'—'}</td><td><span className={'operator-status '+(eligible?'good':blocked?'bad':'pending')}>{eligible?'✓':blocked?'!':'◷'} &nbsp;{eligible?'Verified':blocked?'Rejected':'Pending'}</span></td><td><span className="document-mark" title="Registration and hydro-test records">▤ <i>•</i></span><span className="document-mark" title="Hydro-test record">▤ <i className={blocked?'fail':''}>•</i></span></td><td><button className={eligible?'operator-row-view':'operator-row-verify'} onClick={e=>verify(e,j.registrationNumber)}>{eligible?'View':'Verify'}</button></td></tr>})}</tbody></table>{!filtered.length&&<div className="operator-empty">{journeys.length?'No vehicles match these filters.':'No station journeys have been recorded yet.'}</div>}</div><div className="operator-table-foot">Showing {filtered.length} of {journeys.length} station records</div></section><div className="verification-lower"><section className="operator-panel stats-panel"><div className="operator-panel-title"><div><h2>Verification Statistics</h2><p>Compliance outcomes in loaded station records</p></div></div><div className="stats-chart-row"><div className="donut-chart" style={{'--verified':total?verified/total*100:0,'--pending':total?pending/total*100:0}}><div><b>{total}</b><small>Vehicles</small></div></div><div className="stats-legend"><div><i className="green-dot"/>Verified <b>{verified}</b><small>{total?Math.round(verified/total*100):0}%</small></div><div><i className="amber-dot"/>Pending <b>{pending}</b><small>{total?Math.round(pending/total*100):0}%</small></div><div><i className="red-dot"/>Rejected <b>{rejected}</b><small>{total?Math.round(rejected/total*100):0}%</small></div></div></div></section><section className="operator-panel fuel-panel"><div className="operator-panel-title"><div><h2>Fuel Type Distribution</h2></div></div>{['CNG','LNG','Other'].map(type=>{const count=journeys.filter(j=>String(j.fuelType||j.fuel||'CNG').toUpperCase()===(type==='Other'?'OTHER':type)).length;return <div className="fuel-distribution-row" key={type}><span>{type}</span><i><b style={{width:`${total?count/total*100:0}%`}}/></i><strong>{count}</strong><small>{total?Math.round(count/total*100):0}%</small></div>})}</section><section className="operator-panel recent-verifications"><div className="operator-panel-title"><div><h2>Recent Verification Activity</h2></div><button onClick={loadJourneys}>View all →</button></div><div className="verification-activity-list">{journeys.slice(0,5).map(j=><div key={j.id}><i className={isEligible(j)?'green-dot':'amber-dot'}/><span>Vehicle <b>{j.registrationNumber||'Unknown'}</b> {isEligible(j)?'verified':'awaiting verification'}</span><time>{j.entryTime?new Date(j.entryTime).toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}):'—'}</time></div>)}{!journeys.length&&<small className="operator-subtle">New compliance events will appear here.</small>}</div></section></div></div><aside className="operator-panel verify-side-panel"><div className="operator-panel-title"><div><h2>Verify Vehicle</h2><p>Check a registration with station services</p></div><div style={{display:'flex',gap:'6px',flexWrap:'wrap'}}><input type="file" ref={fileInputRef} accept="image/*" style={{display:'none'}} onChange={handleImageUpload}/><button type="button" className="rfid-button" disabled={uploadBusy||loading} onClick={()=>fileInputRef.current?.click()}>{uploadBusy?'Scanning…':'📁 Photo'}</button><button type="button" className="rfid-button" onClick={()=>{setRfidMessage('RFID reader is not connected. Enter the registration manually.');inputRef.current?.focus()}}>▣ &nbsp;Scan RFID</button></div></div><form className="verify-search-card" onSubmit={verify}><label htmlFor="verify-plate">Vehicle Number</label><div><input id="verify-plate" ref={inputRef} required maxLength={14} value={query} onChange={e=>{setQuery(e.target.value.toUpperCase().replace(/[\s-]/g,''));setRfidMessage('')}} placeholder="e.g. GJ01AB1234"/><button aria-label="Verify vehicle" disabled={loading}>⌕</button></div><button className="verify-submit" disabled={loading}>{loading?'Checking vehicle…':'Verify vehicle →'}</button></form>{rfidMessage&&<div className="rfid-message">{rfidMessage}</div>}{loading?<div className="verify-detail-state"><span className="spinner"/><b>Checking vehicle records…</b></div>:error?<div className="verify-result-error"><b>Verification unavailable</b><span>{error}</span><button onClick={e=>verify(e)}>Try again</button></div>:notFound?<div className="verify-detail-state"><b>Vehicle not found</b><span>No registry record for {query}.</span></div>:displayed?<div className="verified-details"><div className="verified-plate-head"><span className="vehicle-symbol">▣</span><b>{displayed.plate}</b><span className={'operator-status '+(displayed.decision==='CLEARED'?'good':'bad')}>{displayed.decision==='CLEARED'?'✓ Verified':'! Rejected'}</span></div><div className="detail-rows"><div><span>Fuel Type</span><b>{displayed.fuel||'—'}</b></div><div><span>Driver / Owner</span><b>{displayed.owner||'—'}</b></div><div><span>RTO</span><b>{displayed.rto||'—'}</b></div><div><span>Hydro Test</span><b className={displayed.checks?.['Hydro Test']==='VALID'?'text-good':'text-warn'}>{displayed.checks?.['Hydro Test']||'UNKNOWN'}</b></div><div><span>Hydro Expiry</span><b>{displayed.hydroExpiry||'—'}</b></div><div><span>Registration</span><b>{displayed.checks?.Registration||'UNKNOWN'}</b></div></div><div className={'decision-banner '+(displayed.decision==='CLEARED'?'cleared':'blocked')}>{displayed.decision==='CLEARED'?'CLEARED FOR FUELING':'FUELING NOT ALLOWED'}</div><button className="verify-full-details" onClick={()=>document.querySelector('.verified-details')?.scrollIntoView({behavior:'smooth',block:'nearest'})}>View Full Details →</button></div>:<div className="verify-placeholder"><span>▣</span><b>Ready to verify</b><p>Vehicle registration, hydro-test and fueling eligibility details will appear here.</p></div>}</aside></div></div>}
function Alerts({stationId}){const [rows,setRows]=useState([]),[error,setError]=useState(''),[loading,setLoading]=useState(true);async function refresh(){setLoading(true);try{const list=await alertApi.list(stationId);setRows(Array.isArray(list)?list:(Array.isArray(list?.content)?list.content:[]));setError('')}catch(e){setError(e.message)}finally{setLoading(false)}}useEffect(()=>{refresh()},[stationId]);async function resolve(id){try{await alertApi.resolve(id);await refresh()}catch(e){setError(e.message)}}const open=rows.filter(a=>['OPEN','IN_PROGRESS'].includes(String(a.status).toUpperCase()));const high=rows.filter(a=>['HIGH','CRITICAL'].includes(String(a.severity).toUpperCase())&&['OPEN','IN_PROGRESS'].includes(String(a.status).toUpperCase()));return <><PageTitle eyebrow="STATION CONTROL" title="Alerts" sub="Review and resolve events from the station compliance service." action={<button className="btn-outline" onClick={refresh}>↻ &nbsp;Refresh</button>}/><Metrics items={[{label:'OPEN ALERTS',value:open.length,note:'Across all categories'},{label:'HIGH PRIORITY',value:high.length,note:'Requires immediate review',tone:'bad'},{label:'RESOLVED',value:rows.length-open.length,note:'In loaded alert history'}]}/>{error&&<div className="service-notice">{error}</div>}<div className="panel table-panel"><div className="table-toolbar"><div><h3>Alert history</h3><p>Live station events</p></div><button className="btn-outline" onClick={refresh}>⌕ &nbsp;Refresh alerts</button></div><table><thead><tr><th>SEVERITY</th><th>ALERT</th><th>TIME</th><th>STATUS</th><th>ACTION</th></tr></thead><tbody>{rows.map(a=><tr key={a.id}><td><Badge tone={['HIGH','CRITICAL'].includes(String(a.severity).toUpperCase())?'fail':String(a.severity).toUpperCase()==='MEDIUM'?'warn':'neutral'}>{a.severity}</Badge></td><td><b>{String(a.type||'').replaceAll('_',' ')}</b><small>{a.message}</small></td><td>{a.createdAt?new Date(a.createdAt).toLocaleString('en-GB'):'—'}</td><td><Badge tone={String(a.status).toUpperCase()==='RESOLVED'?'pass':'warn'}>{a.status}</Badge></td><td>{['OPEN','IN_PROGRESS'].includes(String(a.status).toUpperCase())&&<button className="btn-outline" onClick={()=>resolve(a.id)}>Resolve</button>}</td></tr>)}</tbody></table>{loading?<div className="table-foot">Loading alerts…</div>:!rows.length&&<div className="table-foot">No alerts have been recorded for this station.</div>}</div></>}
function Compliance({stationId}){const [metrics,setMetrics]=useState(null),[journeys,setJourneys]=useState([]),[query,setQuery]=useState(''),[error,setError]=useState('');useEffect(()=>{let active=true;Promise.all([dashboardApi.metrics(stationId),journeyApi.list(stationId)]).then(([m,j])=>{if(active){setMetrics(m);setJourneys(Array.isArray(j)?j:[])}}).catch(e=>active&&setError(e.message));return()=>{active=false}},[stationId]);const records=journeys.filter(j=>!query||String(j.registrationNumber).includes(query.toUpperCase()));return <><PageTitle eyebrow="COMPLIANCE & ELIGIBILITY" title="Compliance" sub="Vehicle eligibility and regulatory outcomes from the connected service."/><Metrics items={[{label:'VEHICLES CHECKED',value:metrics?.totalVehiclesProcessedToday??'—',note:'Today'},{label:'ELIGIBLE',value:metrics?.eligibleVehicles??'—',note:'Cleared for fueling',tone:'good'},{label:'BLOCKED',value:metrics?.blockedVehicles??'—',note:'Compliance failed',tone:'bad'},{label:'HYDRO TEST ISSUES',value:(metrics?.expiredHydroTestCount??0)+(metrics?.missingHydroTestCount??0),note:`${metrics?.expiredHydroTestCount??0} expired · ${metrics?.missingHydroTestCount??0} missing`,tone:'bad'}]}/>{error&&<div className="service-notice">{error}</div>}<div className="filterbar"><label>Station <span>Connected station</span></label><label>Registration <input value={query} onChange={e=>setQuery(e.target.value.replace(/[\s-]/g,'').toUpperCase())} placeholder="Search plate number"/></label></div><div className="panel table-panel"><div className="table-toolbar"><div><h3>Recent journeys</h3><p>Compliance outcomes recorded by station operations</p></div><span className="muted">{records.length} records</span></div><table><thead><tr><th>VEHICLE</th><th>ENTRY</th><th>JOURNEY STATUS</th><th>COMPLIANCE</th><th>BAY</th></tr></thead><tbody>{records.map(j=><tr key={j.id}><td><Plate plate={j.registrationNumber||'UNKNOWN'}/></td><td>{j.entryTime?new Date(j.entryTime).toLocaleString('en-GB'):'—'}</td><td>{String(j.status||'—').replaceAll('_',' ')}</td><td><Badge tone={String(j.complianceStatus).toUpperCase()==='ELIGIBLE'?'pass':'fail'}>{String(j.complianceStatus||'UNKNOWN').replaceAll('_',' ')}</Badge></td><td>{j.assignedBayNumber??'—'}</td></tr>)}</tbody></table>{!records.length&&<div className="table-foot">No matching journeys are available.</div>}</div><div className="compliance-note"><span>i</span><span><b>Unknown is not compliant.</b> Keep fueling blocked until all required registration and hydro-test records are confirmed.</span></div></>}
function Audit({stationId}){const [pageData,setPageData]=useState(null),[error,setError]=useState('');useEffect(()=>{let active=true;auditApi.list(stationId,'size=50&page=0').then(d=>active&&setPageData(d)).catch(e=>active&&setError(e.message));return()=>{active=false}},[stationId]);const rows=pageData?.content||[];return <><PageTitle eyebrow="GOVERNANCE" title="Audit logs" sub="A traceable record of operator and system activity."/><div className="panel table-panel"><div className="table-toolbar"><div><h3>Activity history</h3><p>Immutable station event record</p></div><span className="muted">{pageData?.totalElements??'—'} entries</span></div>{error&&<div className="service-notice">{error}</div>}<table><thead><tr><th>TIMESTAMP</th><th>ENTITY</th><th>ACTION</th><th>DETAILS</th><th>USER</th></tr></thead><tbody>{rows.map(r=><tr key={r.id}><td>{r.timestamp?new Date(r.timestamp).toLocaleString('en-GB'):'—'}</td><td>{r.entityType||'—'} {r.entityId?`· ${r.entityId}`:''}</td><td><b>{r.action||'—'}</b></td><td>{r.details||r.description||'—'}</td><td>{r.username||r.userName||r.userId||'System'}</td></tr>)}</tbody></table><div className="table-foot">{rows.length?`Showing ${rows.length} of ${pageData?.totalElements??rows.length} entries`:'No audit entries are available.'}</div></div></>}
function Login({onLogin,error,loading}){const [username,setUsername]=useState(''),[password,setPassword]=useState(''),[remember,setRemember]=useState(true),[showPassword,setShowPassword]=useState(false);return <div className="login-shell"><section className="login-visual" aria-label="Sabarmati Gas station operations"><div className="login-brand"><div className="brand-mark"><b>S</b><span>GAS</span></div><div className="brand-name">SABARMATI GAS<span>LIMITED</span></div></div><div className="login-tagline"><span className="leaf-mark">✦</span> CNG for a Cleaner, Safer Tomorrow</div><div className="login-message"><div className="eyebrow"><i/> STATION OPERATIONS PLATFORM</div><h1>Smarter Stations.<br/><em>Safer Journeys.</em></h1><p>AI-powered vehicle verification and compliance<br className="desktop-break"/> for a cleaner, safer and more efficient CNG network<br className="desktop-break"/> across India.</p></div><div className="login-detection" aria-hidden="true"><span className="detection-live"><i/> Vehicle Detected</span><strong>GJ01AB1234</strong><span className="detection-row">▣ <span>Registration</span><b>✓</b> Verified</span><span className="detection-row">▣ <span>Hydro Test</span><b>✓</b> Valid</span><span className="detection-row">▣ <span>Compliance</span><b>✓</b> Eligible</span></div><div className="login-benefits"><div><span className="benefit-icon coral">♧</span><b>100%</b><small>Automated Verification</small></div><div><span className="benefit-icon rose">⛨</span><b>High</b><small>Safety &amp; Compliance</small></div><div><span className="benefit-icon coral">ϟ</span><b>Faster</b><small>Station Operations</small></div><div><span className="benefit-icon green">⌁</span><b>Cleaner</b><small>Greener Tomorrow</small></div></div></section><section className="login-form-wrap"><form className="login-form" onSubmit={e=>{e.preventDefault();onLogin({username,password,remember})}}><div className="form-brand"><div className="brand-mark"><b>S</b><span>GAS</span></div><div className="brand-name">SABARMATI GAS<span>LIMITED</span></div></div><h2>Welcome back</h2><p>Sign in to your operator account to access<br className="desktop-break"/> station operations.</p><label>USERNAME<div className="login-input-wrap"><span aria-hidden="true">♙</span><input autoComplete="username" value={username} onChange={e=>setUsername(e.target.value)} placeholder="Enter your username" required/></div></label><label>PASSWORD<div className="login-input-wrap"><span aria-hidden="true">♙</span><input autoComplete="current-password" type={showPassword?'text':'password'} value={password} onChange={e=>setPassword(e.target.value)} placeholder="Enter your password" required/><button type="button" className="password-toggle" aria-label={showPassword?'Hide password':'Show password'} onClick={()=>setShowPassword(v=>!v)}>{showPassword?'◉':'◎'}</button></div></label>{error&&<div className="login-error" role="alert">{error}</div>}<div className="login-options"><label><input type="checkbox" checked={remember} onChange={e=>setRemember(e.target.checked)}/> Keep me signed in</label><button type="button" className="forgot-password" title="Contact your station administrator to reset your password">Forgot password?</button></div><button type="submit" className="btn-primary full" disabled={loading}>{loading?'Signing in…':'Sign in to operations →'}</button><div className="login-trust"><span><b>♢</b> Secure Access</span><span><b>♧</b> SGL Employees Only</span><span><b>▢</b> Protected Platform</span></div></form><div className="copyright">© 2026 Sabarmati Gas Limited <span>Privacy · Support</span></div></section></div>}
function App(){const [page,setPage]=useState('Overview'),[authed,setAuthed]=useState(!!localStorage.getItem('sgl_access_token')),[authLoading,setAuthLoading]=useState(false),[authError,setAuthError]=useState(''),[stationId,setStationId]=useState(Number(import.meta.env.VITE_STATION_ID)||1),[stations,setStations]=useState([]),[appError,setAppError]=useState(''),[sidebarOpen,setSidebarOpen]=useState(()=>window.innerWidth>900),[scrolled,setScrolled]=useState(false);async function login(credentials){setAuthLoading(true);setAuthError('');try{const result=await authApi.login(credentials);if(!result?.token)throw new Error('The service did not return an access token.');localStorage.setItem('sgl_access_token',result.token);localStorage.setItem('sgl_user_role',result.role||'OPERATOR');localStorage.setItem('sgl_username',credentials.username);setAuthed(true)}catch(err){setAuthError(err.status===500?'The sign-in service encountered an internal error. Please check that the station backend and database are running.':err.message||'Sign in failed.')}finally{setAuthLoading(false)}}function logout(){localStorage.removeItem('sgl_access_token');localStorage.removeItem('sgl_user_role');localStorage.removeItem('sgl_username');setAuthed(false);setPage('Overview')}function navigateTo(next){setPage(next);if(window.innerWidth<=900)setSidebarOpen(false);window.scrollTo({top:0,behavior:'smooth'})}useEffect(()=>{const onScroll=()=>setScrolled(window.scrollY>10);window.addEventListener('scroll',onScroll,{passive:true});return()=>window.removeEventListener('scroll',onScroll)},[]);useEffect(()=>{const expire=()=>{setAuthed(false);setAuthError('Your session has expired. Please sign in again.')};window.addEventListener('sgl:unauthorized',expire);return()=>window.removeEventListener('sgl:unauthorized',expire)},[]);useEffect(()=>{if(!authed)return;let active=true;stationApi.list().then(list=>{if(active&&Array.isArray(list)&&list.length){setStations(list);if(!import.meta.env.VITE_STATION_ID)setStationId(list[0].id)}}).catch(err=>{if(active)setAppError(err.message)});return()=>{active=false}},[authed]);useEffect(()=>{const nodes=document.querySelectorAll('.metric,.panel,.verify-card,.alert-preview,.pipeline,.table-panel,.filterbar,.side-panel,.compliance-note,.overview-card,.overview-stat,.journey-flow');if(!('IntersectionObserver'in window)){nodes.forEach(node=>node.classList.add('is-visible'));return}const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');observer.unobserve(entry.target)}}),{threshold:.06,rootMargin:'0px 0px -18px 0px'});nodes.forEach(node=>{node.classList.add('scroll-reveal');observer.observe(node)});return()=>observer.disconnect()},[page,authed]);if(!authed)return <Login onLogin={login} error={authError} loading={authLoading}/>;return <div className={'app-shell '+(page==='Overview'?'overview-theme ':'')+(page==='Live Monitoring'?'monitor-theme ':'')+(page==='Bays & Queue'?'bay-console-theme ':'')+(['Vehicle Verification','Alerts','Compliance','Audit Logs'].includes(page)?'operator-theme ':'')+(sidebarOpen?'sidebar-open':'sidebar-closed')}><Sidebar page={page} setPage={navigateTo} open={sidebarOpen} onClose={()=>setSidebarOpen(false)}/><main className="main"><Topbar page={page} onLogout={logout} station={stations.find(item=>Number(item.id)===Number(stationId))} stations={stations} onStationChange={id=>setStationId(Number(id))} navOpen={sidebarOpen} onToggleNav={()=>setSidebarOpen(v=>!v)} scrolled={scrolled} onAlerts={()=>navigateTo('Alerts')} onSearch={()=>navigateTo('Vehicle Verification')}/><div className="content">{appError&&<div className="service-notice" role="status">{appError}</div>}{page==='Overview'?<Overview setPage={navigateTo} stationId={stationId}/>:page==='Live Monitoring'?<LivePage stationId={stationId} setPage={navigateTo}/>:page==='Bays & Queue'?<BayOperations stationId={stationId}/>:page==='Vehicle Verification'?<VerifyPage stationId={stationId}/>:page==='Alerts'?<Alerts stationId={stationId}/>:page==='Compliance'?<Compliance stationId={stationId}/>:<Audit stationId={stationId}/>}</div></main></div>}
createRoot(document.getElementById('root')).render(<App/>);
