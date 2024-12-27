import{B as a,Ba as se,Bb as Ee,C as r,D as u,E as x,Ea as ae,Fa as le,G as h,H as m,Ha as ce,Ia as me,Ja as ge,Ka as de,Ma as w,N as y,Na as pe,O as g,Oa as ue,P as I,Pa as T,Q as b,Qa as he,Ra as _e,S as Y,T as B,Ta as P,U as O,Ua as fe,V,Va as Ie,Wa as be,Xa as ve,Y as C,Ya as M,Z as A,Za as xe,_ as J,a as N,ab as Ce,b as Q,ba as X,c as W,ca as R,d as j,da as Z,db as E,e as L,ea as k,eb as Se,f as D,fb as ye,g as F,ga as ee,gb as Ae,h as $,hb as Re,i as H,j as G,k as U,kb as ke,lb as we,m as z,ma as te,nb as Te,o as _,p as f,q,qa as ie,rb as Pe,sa as ne,t as s,u as v,ub as Me,v as S,va as oe,w as K,x as p,z as c,za as re}from"./chunk-KMY4ZES7.js";import"./chunk-6IQPZO6F.js";import"./chunk-JSAAGAZE.js";import"./chunk-5XVA5RR2.js";import"./chunk-LOHYXAXQ.js";import"./chunk-P5VFIEEE.js";import"./chunk-HC6MZPB3.js";import"./chunk-J5OMF6R4.js";import"./chunk-KWW2H4SN.js";import"./chunk-7HA5TF5T.js";import"./chunk-ZOBGMWXG.js";import"./chunk-426OJ4HC.js";import"./chunk-RPQZFOYD.js";import"./chunk-GIGBYVJT.js";import"./chunk-MM5QLNJM.js";import"./chunk-H3GX5QFY.js";import"./chunk-7D6K5XYM.js";import"./chunk-OBXDPQ3V.js";import"./chunk-G2DZN4YG.js";import"./chunk-MCRJI3T3.js";import"./chunk-TI7J2N3O.js";import"./chunk-TTFYQ64X.js";import"./chunk-2R6CW7ES.js";function je(n,d){if(n&1&&(a(0,"ion-col")(1,"small"),g(2),r()()),n&2){let e=m();s(2),I(e.getTeamText())}}var Fe=(()=>{class n{constructor(){}athletregistration;vereinregistration;status;programmlist;teams;selected=new q;statusBadgeColor(){return this.status==="in sync"?"success":"warning"}statusComment(){return this.status==="in sync"?"":this.status.substring(this.status.indexOf("(")+1,this.status.length-1)}statusBadgeText(){return this.status==="in sync"?"\u2714 "+this.status:"\u2757 "+this.status.substring(0,this.status.indexOf("(")-1)}ngOnInit(){}getProgrammText(){let e=this.programmlist.find(t=>t.id===this.athletregistration.programId);return e?e.name:"..."}mapTeam(e){return[...this.teams.filter(t=>t.name?.trim().length>0&&t.index==e).map(t=>t.index>0?t.name+" "+t.index:t.name),""][0]}getTeamText(){if(this.athletregistration.team){let t=this.getProgrammText();return"Team "+this.mapTeam(this.athletregistration.team)+" (bei "+this.athletregistration.geschlecht+"/"+t+")"}else return""}static \u0275fac=function(t){return new(t||n)};static \u0275cmp=S({type:n,selectors:[["app-reg-athlet-item"]],inputs:{athletregistration:"athletregistration",vereinregistration:"vereinregistration",status:"status",programmlist:"programmlist",teams:"teams"},outputs:{selected:"selected"},standalone:!1,decls:19,vars:10,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["no-padding",""],[4,"ngIf"],["slot","end",3,"color"]],template:function(t,i){t&1&&(a(0,"ion-item",0),h("click",function(){return i.selected?i.selected.emit(i.athletregistration):{}}),a(1,"ion-avatar",1),u(2,"img",2),r(),a(3,"ion-grid",3)(4,"ion-row")(5,"ion-col")(6,"ion-label"),g(7),u(8,"br"),a(9,"small"),g(10),C(11,"date"),r()()()(),a(12,"ion-row"),p(13,je,3,1,"ion-col",4),r()(),a(14,"ion-badge",5),g(15),u(16,"br"),a(17,"small"),g(18),r()()()),t&2&&(s(7),Y("",i.athletregistration.name,", ",i.athletregistration.vorname," (",i.athletregistration.geschlecht,")"),s(3),b("(",A(11,8,i.athletregistration.gebdat),")"),s(3),c("ngIf",i.athletregistration.team!=0),s(),c("color",i.statusBadgeColor()),s(),I(i.statusBadgeText()),s(3),I(i.statusComment()))},dependencies:[R,ce,me,w,T,P,M,E,k],encapsulation:2})}return n})();function $e(n,d){if(n&1&&(a(0,"ion-select-option",11),g(1),C(2,"date"),r()),n&2){let e=d.$implicit;c("value",e.uuid),s(),b(" ",e.titel+" "+J(2,2,e.datum,"dd-MM-yy"),"")}}function He(n,d){if(n&1){let e=x();a(0,"ion-select",9),V("ngModelChange",function(i){_(e);let o=m(2);return O(o.competition,i)||(o.competition=i),f(i)}),p(1,$e,3,5,"ion-select-option",10),r()}if(n&2){let e=m(2);B("ngModel",e.competition),s(),c("ngForOf",e.getCompetitions())}}function Ge(n,d){if(n&1&&(a(0,"ion-col")(1,"ion-item"),p(2,He,2,2,"ion-select",8),r()()),n&2){let e=m();s(2),c("ngIf",e.getCompetitions().length>0)}}function Ue(n,d){if(n&1&&(a(0,"ion-note",12),g(1),r()),n&2){let e=m();s(),b(" ",e.competitionName()," ")}}function ze(n,d){n&1&&(a(0,"ion-item")(1,"ion-label"),g(2,"loading ..."),r(),u(3,"ion-spinner"),r())}function qe(n,d){if(n&1){let e=x();a(0,"ion-item-option",19),h("click",function(){_(e);let i=m().$implicit,o=y(1),l=m(3);return f(l.delete(i,o))}),u(1,"ion-icon",17),g(2," L\xF6schen "),r()}}function Ke(n,d){if(n&1){let e=x();a(0,"ion-item-sliding",null,0)(2,"app-reg-athlet-item",14),h("click",function(){let i=_(e).$implicit,o=y(1),l=m(3);return f(l.itemTapped(i,o))}),r(),a(3,"ion-item-options",15)(4,"ion-item-option",16),h("click",function(){let i=_(e).$implicit,o=y(1),l=m(3);return f(l.edit(i,o))}),u(5,"ion-icon",17),g(6),r(),p(7,qe,3,0,"ion-item-option",18),r()()}if(n&2){let e=d.$implicit,t=m(3);s(2),c("athletregistration",e)("vereinregistration",t.currentRegistration)("status",t.getStatus(e))("programmlist",t.wkPgms)("teams",t.teams),s(4),b(" ",t.isLoggedInAsClub()?"Bearbeiten":t.isLoggedIn()?"Best\xE4tigen":"Anzeigen"," "),s(),c("ngIf",t.isLoggedInAsClub())}}function Ye(n,d){if(n&1&&(a(0,"div")(1,"ion-item-divider"),g(2),r(),p(3,Ke,8,7,"ion-item-sliding",13),r()),n&2){let e=d.$implicit,t=m(2);s(2),I(e.name),s(),c("ngForOf",t.filteredStartList(e.id))}}function Je(n,d){if(n&1&&(a(0,"ion-content")(1,"ion-list"),p(2,ze,4,0,"ion-item",4),C(3,"async"),p(4,Ye,4,2,"div",13),r()()),n&2){let e=m();s(2),c("ngIf",A(3,2,e.busy)||!(e.competition&&e.currentRegistration&&e.filteredPrograms)),s(2),c("ngForOf",e.filteredPrograms)}}function Xe(n,d){if(n&1){let e=x();a(0,"ion-button",20),h("click",function(){_(e);let i=m();return f(i.createRegistration())}),u(1,"ion-icon",21),g(2," Neue Anmeldung... "),r()}}var Be=(()=>{class n{navCtrl;route;backendService;alertCtrl;busy=new Q(!1);currentRegistration;teams;currentRegId;wkPgms;tMyQueryStream=new N;sFilterTask=void 0;sFilteredRegistrationList;sAthletRegistrationList;sMyQuery;sSyncActions=[];constructor(e,t,i,o){this.navCtrl=e,this.route=t,this.backendService=i,this.alertCtrl=o,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);let e=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),e?this.competition=e:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.refreshList()}getSyncActions(){this.backendService.loadRegistrationSyncActions().pipe(F(1)).subscribe(e=>{this.sSyncActions=e})}getStatus(e){if(this.sSyncActions){let t=this.sSyncActions.find(i=>i.verein.id===e.vereinregistrationId&&i.caption.indexOf(e.name)>-1&&i.caption.indexOf(e.vorname)>-1);return t?"pending ("+t.caption.substring(0,(t.caption+":").indexOf(":"))+")":"in sync"}else return"n/a"}refreshList(){this.busy.next(!0),this.currentRegistration=void 0,this.getSyncActions(),this.backendService.getClubRegistrations(this.competition).pipe(L(e=>!!e.find(t=>t.id===this.currentRegId)),F(1)).subscribe(e=>{this.currentRegistration=e.find(t=>t.id===this.currentRegId),this.backendService.loadTeamsListForClub(this.competition,this.currentRegId).subscribe(t=>{this.teams=t}),console.log("ask athletes-list for registration"),this.backendService.loadAthletRegistrations(this.competition,this.currentRegId).subscribe(t=>{this.busy.next(!1),this.athletregistrations=t,this.tMyQueryStream.pipe(L(o=>!!o&&!!o.target),j(o=>o.target.value||"*"),D(300),$(),U(o=>this.busy.next(!0)),G(this.runQuery(t)),H()).subscribe(o=>{this.sFilteredRegistrationList=o,this.busy.next(!1)})})})}set competition(e){(!this.currentRegistration||e!==this.backendService.competition)&&this.backendService.loadProgramsForCompetition(this.competition).subscribe(t=>{this.wkPgms=t})}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}runQuery(e){return t=>{let i=t.trim(),o=new Map;return e&&e.forEach(l=>{if(this.filter(i)(l)){let Oe=o.get(l.programId)||[];o.set(l.programId,[...Oe,l].sort((Ve,Ne)=>Ve.name.localeCompare(Ne.name)))}}),W(o)}}set athletregistrations(e){this.sAthletRegistrationList=e,this.runQuery(e)("*").subscribe(t=>this.sFilteredRegistrationList=t),this.reloadList(this.sMyQuery||"*")}get athletregistrations(){return this.sAthletRegistrationList}get filteredPrograms(){return[...this.wkPgms.filter(e=>this.sFilteredRegistrationList?.has(e.id))]}mapProgram(e){return this.wkPgms.filter(t=>t.id==e)[0]}filteredStartList(e){return this.sFilteredRegistrationList?.get(e)||this.sAthletRegistrationList||[]}reloadList(e){this.tMyQueryStream.next(e)}itemTapped(e,t){t.getOpenAmount().then(i=>{i>0?t.close():t.open("end")})}isLoggedInAsClub(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""}isLoggedInAsAdmin(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}isLoggedIn(){return this.backendService.loggedIn}filter(e){let t=e.toUpperCase().split(" ");return i=>e.trim()==="*"||t.filter(o=>{if(i.name.toUpperCase().indexOf(o)>-1||i.vorname.toUpperCase().indexOf(o)>-1||this.wkPgms.find(l=>i.programId===l.id&&l.name===o)||i.gebdat.indexOf(o)>-1||o.length==3&&i.geschlecht.indexOf(o.substring(1,2))>-1)return!0}).length===t.length}createRegistration(){this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/0`)}edit(e,t){this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/${e.id}`),t.close()}needsPGMChoice(){let e=[...this.wkPgms][0];return!(e.aggregate==1&&e.riegenmode>1)}similarRegistration(e,t){return e.athletId===t.athletId||e.name===t.name&&e.vorname===t.vorname&&e.gebdat===t.gebdat&&e.geschlecht===t.geschlecht}delete(e,t){t.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xF6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+e.name+", "+e.vorname+" am Wettkampf gel\xF6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.needsPGMChoice()||this.athletregistrations.filter(o=>this.similarRegistration(o,e)).filter(o=>o.id!==e.id).forEach(o=>{this.backendService.deleteAthletRegistration(this.backendService.competition,this.currentRegId,o)}),this.backendService.deleteAthletRegistration(this.backendService.competition,this.currentRegId,e).subscribe(()=>{this.refreshList()})}}]}).then(o=>o.present())}static \u0275fac=function(t){return new(t||n)(v(se),v(te),v(Ee),v(Pe))};static \u0275cmp=S({type:n,selectors:[["app-reg-athletlist"]],standalone:!1,decls:18,vars:5,consts:[["slidingAthletRegistrationItem",""],["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModelChange","ionInput","ionCancel","ngModel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],[3,"click","athletregistration","vereinregistration","status","programmlist","teams"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(t,i){t&1&&(a(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1),u(3,"ion-back-button",2),r(),a(4,"ion-title")(5,"ion-grid",3)(6,"ion-row")(7,"ion-col")(8,"ion-label"),g(9,"Angemeldete Athleten/Athletinnen"),r()(),p(10,Ge,3,1,"ion-col",4),r()()(),p(11,Ue,2,1,"ion-note",5),r(),a(12,"ion-searchbar",6),V("ngModelChange",function(l){return O(i.sMyQuery,l)||(i.sMyQuery=l),l}),h("ionInput",function(l){return i.reloadList(l)})("ionCancel",function(l){return i.reloadList(l)}),r()(),p(13,Je,5,4,"ion-content",4),a(14,"ion-footer")(15,"ion-toolbar")(16,"ion-list"),p(17,Xe,3,0,"ion-button",7),r()()()),t&2&&(s(10),c("ngIf",!i.competition),s(),c("ngIf",i.competition),s(),B("ngModel",i.sMyQuery),s(),c("ngIf",i.wkPgms&&i.wkPgms.length>0),s(4),c("ngIf",i.isLoggedInAsClub()))},dependencies:[X,R,ne,oe,ge,de,w,pe,ue,T,he,_e,P,fe,Ie,be,ve,M,xe,Ce,E,Se,ye,Ae,Re,ke,we,ae,le,Te,Fe,Z,k],encapsulation:2})}return n})();var Ze=[{path:"",component:Be}],It=(()=>{class n{static \u0275fac=function(t){return new(t||n)};static \u0275mod=K({type:n});static \u0275inj=z({imports:[ee,re,Me,ie.forChild(Ze)]})}return n})();export{It as RegAthletlistPageModule};
