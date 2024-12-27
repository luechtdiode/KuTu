import{c as ce,h as le}from"./chunk-5JK2MF46.js";import{Ab as T,B as a,Ba as O,Bb as ae,C as s,D as c,E as x,G as b,H as v,I as k,Ja as H,Ka as U,N as I,Na as D,O as d,Oa as K,P as w,Q as y,Qa as Y,Ra as Z,Ta as q,Va as J,Wa as Q,Xa as X,Y as M,Ya as $,Z as P,Za as ee,aa as z,ab as te,ca as L,d as N,da as W,ga as F,kb as ie,lb as ne,m as G,nb as re,o as h,p as m,qa as j,r as A,rb as oe,s as E,t as r,u,ub as se,v as R,w as V,x as f,yb as _,z as o,za as B}from"./chunk-KMY4ZES7.js";import"./chunk-6IQPZO6F.js";import"./chunk-JSAAGAZE.js";import"./chunk-5XVA5RR2.js";import"./chunk-LOHYXAXQ.js";import"./chunk-P5VFIEEE.js";import"./chunk-HC6MZPB3.js";import"./chunk-J5OMF6R4.js";import"./chunk-KWW2H4SN.js";import"./chunk-7HA5TF5T.js";import"./chunk-ZOBGMWXG.js";import"./chunk-426OJ4HC.js";import"./chunk-RPQZFOYD.js";import"./chunk-GIGBYVJT.js";import"./chunk-MM5QLNJM.js";import"./chunk-H3GX5QFY.js";import"./chunk-7D6K5XYM.js";import"./chunk-OBXDPQ3V.js";import"./chunk-G2DZN4YG.js";import"./chunk-MCRJI3T3.js";import"./chunk-TI7J2N3O.js";import"./chunk-TTFYQ64X.js";import{g as C}from"./chunk-2R6CW7ES.js";function pe(n,S){n&1&&(a(0,"div")(1,"ion-title"),d(2,"Resultate"),s()())}function he(n,S){if(n&1&&(a(0,"div")(1,"ion-title"),d(2,"Resultaterfassung"),a(3,"div",14),d(4),s()()()),n&2){let e=v();r(3),o("ngClass",e.durchgangstateClass),r(),w(e.durchgangstate())}}function me(n,S){if(n&1&&(c(0,"riege-list",15),M(1,"async")),n&2){let e=v();o("items",P(1,1,e.getWertungen()))}}function ue(n,S){n&1&&c(0,"ion-icon",16)}function fe(n,S){if(n&1){let e=x();a(0,"ion-button",17),b("click",function(){h(e);let i=v();return m(i.finish())}),c(1,"ion-icon",18),d(2," Eingaben abschliessen "),s()}if(n&2){let e=v();o("disabled",!(e.geraet&&e.getSteps()))}}var de=(()=>{class n{navCtrl;backendService;alertCtrl;zone;durchgangopen=!1;geraete=[];constructor(e,t,i,p){this.navCtrl=e,this.backendService=t,this.alertCtrl=i,this.zone=p,this.backendService.durchgangStarted.pipe(N(g=>g.filter(l=>T(l.durchgang)===T(this.backendService.durchgang)&&l.wettkampfUUID===this.backendService.competition).length>0)).subscribe(g=>{this.durchgangopen=g})}ionViewWillEnter(){this.geraete.length>0&&!this.backendService.captionmode&&this.backendService.activateCaptionMode()}ngOnInit(){this.backendService.geraeteSubject.subscribe(e=>{this.backendService.captionmode&&(this.geraete=e)})}durchgangstate(){return this.backendService.isWebsocketConnected()?this.durchgangopen?"gestartet":"gesperrt":"offline"}get durchgangstateClass(){return{open:this.backendService.isWebsocketConnected()&&this.durchgangopen,closed:this.backendService.isWebsocketConnected()&&!this.durchgangopen,offline:!this.backendService.isWebsocketConnected()}}set competition(e){this.stationFreezed||this.backendService.getDurchgaenge(e)}get competition(){return this.backendService.competition}set durchgang(e){this.stationFreezed||this.backendService.getGeraete(this.competition,e)}get durchgang(){return this.backendService.durchgang}set geraet(e){this.stationFreezed||this.backendService.getSteps(this.competition,this.durchgang,e)}get geraet(){return this.backendService.geraet}set step(e){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,e)}get step(){return this.backendService.step}get stationFreezed(){return this.backendService.stationFreezed}isLoggedIn(){return this.backendService.loggedIn}get nextStepCaption(){return this.isLoggedIn()?"N\xE4chste Riege":"N\xE4chstes Ger\xE4t"}get prevStepCaption(){return this.isLoggedIn()?"Vorherige Riege":"Vorheriges Ger\xE4t"}itemTapped(e){e.getOpenAmount().then(t=>{t>10||t<-10?e.close():e.open("end")})}nextStep(e){this.backendService.nextGeraet().subscribe(t=>{this.step=t,e.close()})}prevStep(e){this.backendService.prevGeraet().subscribe(t=>{this.step=t,e.close()})}get station(){return this.isLoggedIn()?this.geraetName()+" - "+this.step+". Riege von "+this.getSteps()?.length+", "+this.durchgang:this.geraetName()+" - "+this.step+". Ger\xE4t von "+this.getGeraete()?.length+", "+this.durchgang}get previousGearImage(){return this.isLoggedIn()?void 0:_[this.backendService.getPrevGeraet()]||void 0}get nextGearImage(){return this.isLoggedIn()?void 0:_[this.backendService.getNextGeraet()]||void 0}get gearImage(){return _[this.backendService.geraet]||void 0}showCompleteAlert(){return C(this,null,function*(){let e=this.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+this.station+'" k\xF6nnen die erfassten Wertungen nur noch im Wettkampf-B\xFCro korrigiert werden.',buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{let t=e.then(i=>i.dismiss());return this.zone.run(()=>{this.backendService.finishStation(this.competition,this.durchgang,this.geraet,this.step).subscribe(i=>{i.length===0&&t.then(()=>{this.navCtrl.pop()})})}),!1}}]});e.then(t=>t.present())})}toastMissingResult(e){return C(this,null,function*(){let t=e[0].vorname.toUpperCase()+" "+e[0].name.toUpperCase()+" ("+e[0].verein+")";(yield this.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"In dieser Riege gibt es noch "+e.length+" leere Wertungen! - Bitte pr\xFCfen, ob "+t+" geturnt hat.",buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:t+" hat nicht geturnt",role:"edit",handler:()=>{e.splice(0,1),e.length>0?this.toastMissingResult(e):this.showCompleteAlert()}},{text:"Korrigieren",role:"edit",handler:()=>{this.navCtrl.navigateForward("wertung-editor/"+e[0].id)}}]})).present()})}finish(){let e=this.backendService.wertungen.filter(t=>t.wertung.endnote===void 0);if(e.length===0){this.showCompleteAlert();return}else this.toastMissingResult(e)}getCompetitions(){return this.backendService.competitions}competitionName(){if(!this.backendService.competitions)return"";let e=this.backendService.competitions.filter(t=>t.uuid===this.backendService.competition).map(t=>t.titel+"<br>"+(t.datum+"T").split("T")[0].split("-").reverse().join("-"));return e.length===1?e[0]:""}geraetName(){if(!this.geraete||this.geraete.length===0)return"";let e=this.geraete.filter(t=>t.id===this.backendService.geraet).map(t=>t.name);return e.length===1?e[0]:""}getDurchgaenge(){return this.backendService.durchgaenge}getGeraete(){return this.geraete||[]}getSteps(){return this.backendService.steps}getWertungen(){return this.backendService.wertungenSubject}static \u0275fac=function(t){return new(t||n)(u(O),u(ae),u(oe),u(A))};static \u0275cmp=R({type:n,selectors:[["app-station"]],standalone:!1,decls:29,vars:19,consts:[["slidingStepItem",""],["slot","start"],["defaultHref","/"],[4,"ngIf"],["slot","end",1,"ion-padding-end",3,"innerHtml"],[3,"items",4,"ngIf"],[3,"click"],["slot","start",3,"src"],["slot","end","name","arrow-forward-circle-outline",4,"ngIf"],["side","start"],["color","primary",3,"click","disabled"],[3,"src"],["side","end"],["size","large","expand","block","color","success",3,"disabled","click",4,"ngIf"],[1,"stateinfo",3,"ngClass"],[3,"items"],["slot","end","name","arrow-forward-circle-outline"],["size","large","expand","block","color","success",3,"click","disabled"],["slot","start","name","git-commit"]],template:function(t,i){if(t&1){let p=x();a(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1),c(3,"ion-back-button",2),s(),f(4,pe,3,0,"div",3)(5,he,5,2,"div",3),c(6,"ion-note",4),s()(),a(7,"ion-content"),f(8,me,2,3,"riege-list",5),M(9,"async"),s(),a(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list")(13,"ion-item-sliding",null,0)(15,"ion-item",6),b("click",function(){h(p);let l=I(14);return m(i.itemTapped(l))}),c(16,"ion-icon",7),a(17,"ion-label"),d(18),s(),f(19,ue,1,0,"ion-icon",8),s(),a(20,"ion-item-options",9)(21,"ion-item-option",10),b("click",function(){h(p);let l=I(14);return m(i.prevStep(l))}),c(22,"ion-icon",11),d(23),s()(),a(24,"ion-item-options",12)(25,"ion-item-option",10),b("click",function(){h(p);let l=I(14);return m(i.nextStep(l))}),c(26,"ion-icon",11),d(27),s()()(),f(28,fe,3,1,"ion-button",13),s()()()}t&2&&(r(4),o("ngIf",!i.isLoggedIn()),r(),o("ngIf",i.isLoggedIn()),r(),o("innerHtml",i.competitionName(),E),r(2),o("ngIf",i.geraet&&i.durchgang&&i.step&&P(9,17,i.getWertungen())),r(8),k("src","assets/imgs/",i.gearImage,""),r(2),w(i.station),r(),o("ngIf",i.geraet&&i.getSteps()),r(2),o("disabled",!i.geraet||!i.getSteps()),r(),k("src","assets/imgs/",i.previousGearImage,""),r(),y(" ",i.prevStepCaption," "),r(2),o("disabled",!i.geraet||!i.getSteps()),r(),k("src","assets/imgs/",i.nextGearImage,""),r(),y(" ",i.nextStepCaption," "),r(),o("ngIf",i.isLoggedIn()&&i.durchgangopen))},dependencies:[z,L,H,U,D,K,Y,Z,q,J,Q,X,$,ee,te,ie,ne,re,ce,W],styles:[".stateinfo.closed[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-danger)}.stateinfo.open[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-success)}.stateinfo.offline[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-warning)}ion-icon[_ngcontent-%COMP%]{color:var(--ion-card-color);font-size:35px;border-color:var(--ion-item-border-color);border-width:2px;border-style:solid;border-radius:4px;margin-right:16px}"]})}return n})();var be=[{path:"",component:de}],Ee=(()=>{class n{static \u0275fac=function(t){return new(t||n)};static \u0275mod=V({type:n});static \u0275inj=G({imports:[F,B,se,j.forChild(be),le]})}return n})();export{Ee as StationPageModule};
