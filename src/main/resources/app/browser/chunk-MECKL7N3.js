import{d as ke,h as Ae}from"./chunk-5JK2MF46.js";import{$a as ge,B as c,Ba as Z,Bb as Me,C as a,D as g,E as I,Ea as ee,Fa as te,G as f,H as p,Ja as ie,Ka as ne,Ma as oe,N as v,Na as re,O as u,P as W,Pa as ae,Q as y,Qa as le,Ra as ce,T as x,Ta as se,U as C,Ua as me,V as w,Va as pe,Wa as de,Xa as ue,Y as M,Ya as _e,Z as U,Za as he,_ as R,a as A,ab as fe,b as T,ba as D,c as P,ca as $,d as F,da as q,db as Se,e as E,ea as z,eb as ve,f as O,fb as ye,ga as G,gb as be,h as L,hb as Ie,i as B,j as V,kb as xe,lb as Ce,m as N,ma as H,o as _,p as h,qa as J,sa as K,t as l,u as S,ub as we,v as Q,va as X,w as j,x as d,z as s,za as Y}from"./chunk-KMY4ZES7.js";import"./chunk-6IQPZO6F.js";import"./chunk-JSAAGAZE.js";import"./chunk-5XVA5RR2.js";import"./chunk-LOHYXAXQ.js";import"./chunk-P5VFIEEE.js";import"./chunk-HC6MZPB3.js";import"./chunk-J5OMF6R4.js";import"./chunk-KWW2H4SN.js";import"./chunk-7HA5TF5T.js";import"./chunk-ZOBGMWXG.js";import"./chunk-426OJ4HC.js";import"./chunk-RPQZFOYD.js";import"./chunk-GIGBYVJT.js";import"./chunk-MM5QLNJM.js";import"./chunk-H3GX5QFY.js";import"./chunk-7D6K5XYM.js";import"./chunk-OBXDPQ3V.js";import"./chunk-G2DZN4YG.js";import"./chunk-MCRJI3T3.js";import"./chunk-TI7J2N3O.js";import"./chunk-TTFYQ64X.js";import"./chunk-2R6CW7ES.js";function Ee(n,m){if(n&1&&(c(0,"ion-select-option",10),u(1),M(2,"date"),a()),n&2){let e=m.$implicit;s("value",e.uuid),l(),y(" ",e.titel+" "+R(2,2,e.datum,"dd-MM-yy"),"")}}function Oe(n,m){if(n&1){let e=I();c(0,"ion-select",8),w("ngModelChange",function(t){_(e);let o=p(2);return C(o.competition,t)||(o.competition=t),h(t)}),d(1,Ee,3,5,"ion-select-option",9),a()}if(n&2){let e=p(2);x("ngModel",e.competition),l(),s("ngForOf",e.getCompetitions())}}function Le(n,m){if(n&1&&(c(0,"ion-col")(1,"ion-item"),d(2,Oe,2,2,"ion-select",7),a()()),n&2){let e=p();l(2),s("ngIf",e.getCompetitions().length>0)}}function Be(n,m){if(n&1&&(c(0,"ion-note",11),u(1),a()),n&2){let e=p();l(),y(" ",e.competitionName()," ")}}function Ve(n,m){n&1&&(c(0,"ion-item")(1,"ion-label"),u(2,"loading ..."),a(),g(3,"ion-spinner"),a())}function Ne(n,m){if(n&1){let e=I();c(0,"ion-item-sliding",null,0)(2,"startlist-item",13),f("click",function(){let t=_(e).$implicit,o=v(1),r=p(3);return h(r.itemTapped(t,o))}),a(),c(3,"ion-item-options",14)(4,"ion-item-option",15),f("click",function(){let t=_(e).$implicit,o=v(1),r=p(3);return h(r.followAthlet(t,o))}),g(5,"ion-icon",16),u(6),a(),c(7,"ion-item-option",17),f("click",function(){let t=_(e).$implicit,o=v(1),r=p(3);return h(r.followRiege(t,o))}),g(8,"ion-icon",16),u(9," Riege verfolgen "),a()()()}if(n&2){let e=m.$implicit,i=p().$implicit;l(2),s("teilnehmer",e)("programm",i),l(4),y(" ",e.athlet," verfolgen ")}}function Qe(n,m){if(n&1&&(c(0,"div")(1,"ion-item-divider"),u(2),a(),d(3,Ne,10,3,"ion-item-sliding",12),a()),n&2){let e=m.$implicit;l(2),W(e.programm),l(),s("ngForOf",e.teilnehmer)}}function je(n,m){if(n&1&&(c(0,"ion-content")(1,"ion-list"),d(2,Ve,4,0,"ion-item",4),M(3,"async"),d(4,Qe,4,2,"div",12),a()()),n&2){let e=p();l(2),s("ngIf",U(3,2,e.isBusy)),l(2),s("ngForOf",e.filteredStartList.programme)}}var Te=(()=>{class n{navCtrl;route;backendService;sStartList;sFilteredStartList;sMyQuery;tMyQueryStream=new A;sFilterTask=void 0;busy=new T(!1);constructor(e,i,t){this.navCtrl=e,this.route=i,this.backendService=t,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);let e=this.route.snapshot.paramMap.get("wkId");e&&(this.competition=e)}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){(!this.startlist||e!==this.backendService.competition)&&(this.busy.next(!0),this.startlist={},this.backendService.getDurchgaenge(e),this.backendService.loadStartlist(void 0).subscribe(i=>{this.startlist=i,this.busy.next(!1);let t=this.tMyQueryStream.pipe(E(o=>!!o&&!!o.target&&!!o.target.value),F(o=>o.target.value),O(1e3),L(),B());t.subscribe(o=>{this.busy.next(!0)}),t.pipe(V(this.runQuery(i))).subscribe(o=>{this.sFilteredStartList=o,this.busy.next(!1)})}))}get competition(){return this.backendService.competition||""}set startlist(e){this.sStartList=e,this.reloadList(this.sMyQuery)}get startlist(){return this.sStartList}get filteredStartList(){return this.sFilteredStartList||{programme:[]}}get isBusy(){return this.busy}runQuery(e){return i=>{let t=i.trim(),o;return o={programme:[]},t&&e&&e.programme.forEach(r=>{let Pe=this.filter(t),k=r.teilnehmer.filter(b=>Pe(b,r.programm));if(k.length>0){let b={programm:r.programm,teilnehmer:k};o=Object.assign({},e,{programme:[...o.programme,b]})}}),P(o)}}reloadList(e){this.tMyQueryStream.next(e)}itemTapped(e,i){i.getOpenAmount().then(t=>{t>0?i.close():i.open("end")})}followAthlet(e,i){i.close(),this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${e.athletid}`)}followRiege(e,i){i.close(),this.backendService.getGeraete(this.backendService.competition,e.durchgang).subscribe(t=>{this.backendService.getSteps(this.backendService.competition,e.durchgang,t.find(o=>o.name===e.start).id).subscribe(o=>{this.navCtrl.navigateForward("station")})})}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";let e=this.backendService.competitions.filter(i=>i.uuid===this.backendService.competition).map(i=>i.titel+", am "+(i.datum+"T").split("T")[0].split("-").reverse().join("-"));return e.length===1?(this.startlist||(this.competition=this.backendService.competition),e[0]):""}filter(e){let i=e.toUpperCase().split(" ");return(t,o)=>i.filter(r=>{if(t.athletid+""===r||t.athlet.toUpperCase().indexOf(r)>-1||t.verein.toUpperCase().indexOf(r)>-1||t.start.toUpperCase().indexOf(r)>-1||t.verein.toUpperCase().indexOf(r)>-1||t.team.toUpperCase().indexOf(r)>-1||o.indexOf(r)>-1)return!0}).length===i.length}static \u0275fac=function(i){return new(i||n)(S(Z),S(H),S(Me))};static \u0275cmp=Q({type:n,selectors:[["app-search-athlet"]],standalone:!1,decls:16,vars:4,consts:[["slidingAthletItem",""],["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModelChange","ionInput","ionCancel","ngModel"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],[3,"click","teilnehmer","programm"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","secondary",3,"click"]],template:function(i,t){i&1&&(c(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1)(3,"ion-menu-toggle")(4,"ion-button"),g(5,"ion-icon",2),a()()(),c(6,"ion-title")(7,"ion-grid",3)(8,"ion-row")(9,"ion-col")(10,"ion-label"),u(11,"Suche Turner/-in"),a()(),d(12,Le,3,1,"ion-col",4),a()()(),d(13,Be,2,1,"ion-note",5),a(),c(14,"ion-searchbar",6),w("ngModelChange",function(r){return C(t.sMyQuery,r)||(t.sMyQuery=r),r}),f("ionInput",function(r){return t.reloadList(r)})("ionCancel",function(r){return t.reloadList(r)}),a()(),d(15,je,5,4,"ion-content",4)),i&2&&(l(12),s("ngIf",!t.competition),l(),s("ngIf",t.competition),l(),x("ngModel",t.sMyQuery),l(),s("ngIf",t.competition&&t.startlist&&t.startlist.programme))},dependencies:[D,$,K,X,ie,ne,oe,re,ae,le,ce,se,me,pe,de,ue,_e,he,ge,fe,Se,ve,ye,be,Ie,xe,Ce,ee,te,ke,q,z],encapsulation:2})}return n})();var We=[{path:"",component:Te}],it=(()=>{class n{static \u0275fac=function(i){return new(i||n)};static \u0275mod=j({type:n});static \u0275inj=N({imports:[G,Y,we,J.forChild(We),Ae]})}return n})();export{it as SearchAthletPageModule};
