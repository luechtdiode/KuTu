"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8317],{8317:(D,u,l)=>{l.r(u),l.d(u,{SearchAthletPageModule:()=>x});var m=l(177),h=l(4341),d=l(305),s=l(4742),f=l(1413),_=l(4412),S=l(7673),v=l(5964),b=l(6354),F=l(152),k=l(3294),y=l(7647),C=l(5558),t=l(3953),I=l(2872),j=l(2350);let T=(()=>{class o{programm;teilnehmer;selected;constructor(){}ngOnInit(){}static \u0275fac=function(n){return new(n||o)};static \u0275cmp=t.VBU({type:o,selectors:[["startlist-item"]],inputs:{programm:"programm",teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:16,vars:5,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(n,i){1&n&&(t.j41(0,"ion-item",0),t.bIt("click",function(){return i.selected?i.selected.emit(i.teilnehmer):{}}),t.j41(1,"ion-avatar",1),t.nrm(2,"img",2),t.k0s(),t.j41(3,"ion-label"),t.EFF(4),t.j41(5,"small"),t.EFF(6),t.k0s(),t.nrm(7,"br"),t.j41(8,"small")(9,"em"),t.EFF(10),t.k0s()()(),t.j41(11,"ion-note",3),t.EFF(12," Startger\xe4t:"),t.nrm(13,"br"),t.j41(14,"b"),t.EFF(15),t.k0s()()()),2&n&&(t.R7$(4),t.SpI("",i.teilnehmer.athlet,"\xa0"),t.R7$(2),t.JRh(i.teilnehmer.verein+", "+i.programm.programm),t.R7$(4),t.Lme("",i.teilnehmer.durchgang," ",i.teilnehmer.team,""),t.R7$(5),t.JRh(i.teilnehmer.start))},dependencies:[s.mC,s.uz,s.he,s.JI],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]})}return o})();function A(o,c){if(1&o&&(t.j41(0,"ion-select-option",10),t.EFF(1),t.nI1(2,"date"),t.k0s()),2&o){const e=c.$implicit;t.Y8G("value",e.uuid),t.R7$(),t.SpI(" ",e.titel+" "+t.i5U(2,2,e.datum,"dd-MM-yy"),"")}}function M(o,c){if(1&o){const e=t.RV6();t.j41(0,"ion-select",8),t.mxI("ngModelChange",function(i){t.eBV(e);const r=t.XpG(2);return t.DH7(r.competition,i)||(r.competition=i),t.Njj(i)}),t.DNE(1,A,3,5,"ion-select-option",9),t.k0s()}if(2&o){const e=t.XpG(2);t.R50("ngModel",e.competition),t.R7$(),t.Y8G("ngForOf",e.getCompetitions())}}function R(o,c){if(1&o&&(t.j41(0,"ion-col")(1,"ion-item"),t.DNE(2,M,2,2,"ion-select",7),t.k0s()()),2&o){const e=t.XpG();t.R7$(2),t.Y8G("ngIf",e.getCompetitions().length>0)}}function E(o,c){if(1&o&&(t.j41(0,"ion-note",11),t.EFF(1),t.k0s()),2&o){const e=t.XpG();t.R7$(),t.SpI(" ",e.competitionName()," ")}}function $(o,c){1&o&&(t.j41(0,"ion-item")(1,"ion-label"),t.EFF(2,"loading ..."),t.k0s(),t.nrm(3,"ion-spinner"),t.k0s())}function G(o,c){if(1&o){const e=t.RV6();t.j41(0,"ion-item-sliding",null,0)(2,"startlist-item",13),t.bIt("click",function(){const i=t.eBV(e).$implicit,r=t.sdS(1),a=t.XpG(3);return t.Njj(a.itemTapped(i,r))}),t.k0s(),t.j41(3,"ion-item-options",14)(4,"ion-item-option",15),t.bIt("click",function(){const i=t.eBV(e).$implicit,r=t.sdS(1),a=t.XpG(3);return t.Njj(a.followAthlet(i,r))}),t.nrm(5,"ion-icon",16),t.EFF(6),t.k0s(),t.j41(7,"ion-item-option",17),t.bIt("click",function(){const i=t.eBV(e).$implicit,r=t.sdS(1),a=t.XpG(3);return t.Njj(a.followRiege(i,r))}),t.nrm(8,"ion-icon",16),t.EFF(9," Riege verfolgen "),t.k0s()()()}if(2&o){const e=c.$implicit,n=t.XpG().$implicit;t.R7$(2),t.Y8G("teilnehmer",e)("programm",n),t.R7$(4),t.SpI(" ",e.athlet," verfolgen ")}}function P(o,c){if(1&o&&(t.j41(0,"div")(1,"ion-item-divider"),t.EFF(2),t.k0s(),t.DNE(3,G,10,3,"ion-item-sliding",12),t.k0s()),2&o){const e=c.$implicit;t.R7$(2),t.JRh(e.programm),t.R7$(),t.Y8G("ngForOf",e.teilnehmer)}}function O(o,c){if(1&o&&(t.j41(0,"ion-content")(1,"ion-list"),t.DNE(2,$,4,0,"ion-item",4),t.nI1(3,"async"),t.DNE(4,P,4,2,"div",12),t.k0s()()),2&o){const e=t.XpG();t.R7$(2),t.Y8G("ngIf",t.bMT(3,2,e.isBusy)),t.R7$(2),t.Y8G("ngForOf",e.filteredStartList.programme)}}let B=(()=>{class o{navCtrl;route;backendService;sStartList;sFilteredStartList;sMyQuery;tMyQueryStream=new f.B;sFilterTask=void 0;busy=new _.t(!1);constructor(e,n,i){this.navCtrl=e,this.route=n,this.backendService=i,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const e=this.route.snapshot.paramMap.get("wkId");e&&(this.competition=e)}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){(!this.startlist||e!==this.backendService.competition)&&(this.busy.next(!0),this.startlist={},this.backendService.getDurchgaenge(e),this.backendService.loadStartlist(void 0).subscribe(n=>{this.startlist=n,this.busy.next(!1);const i=this.tMyQueryStream.pipe((0,v.p)(r=>!!r&&!!r.target&&!!r.target.value),(0,b.T)(r=>r.target.value),(0,F.B)(1e3),(0,k.F)(),(0,y.u)());i.subscribe(r=>{this.busy.next(!0)}),i.pipe((0,C.n)(this.runQuery(n))).subscribe(r=>{this.sFilteredStartList=r,this.busy.next(!1)})}))}get competition(){return this.backendService.competition||""}set startlist(e){this.sStartList=e,this.reloadList(this.sMyQuery)}get startlist(){return this.sStartList}get filteredStartList(){return this.sFilteredStartList||{programme:[]}}get isBusy(){return this.busy}runQuery(e){return n=>{const i=n.trim();let r;return r={programme:[]},i&&e&&e.programme.forEach(a=>{const L=this.filter(i),g=a.teilnehmer.filter(p=>L(p,a.programm));g.length>0&&(r=Object.assign({},e,{programme:[...r.programme,{programm:a.programm,teilnehmer:g}]}))}),(0,S.of)(r)}}reloadList(e){this.tMyQueryStream.next(e)}itemTapped(e,n){n.getOpenAmount().then(i=>{i>0?n.close():n.open("end")})}followAthlet(e,n){n.close(),this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${e.athletid}`)}followRiege(e,n){n.close(),this.backendService.getGeraete(this.backendService.competition,e.durchgang).subscribe(i=>{this.backendService.getSteps(this.backendService.competition,e.durchgang,i.find(r=>r.name===e.start).id).subscribe(r=>{this.navCtrl.navigateForward("station")})})}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const e=this.backendService.competitions.filter(n=>n.uuid===this.backendService.competition).map(n=>n.titel+", am "+(n.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===e.length?(this.startlist||(this.competition=this.backendService.competition),e[0]):""}filter(e){const n=e.toUpperCase().split(" ");return(i,r)=>n.filter(a=>{if(i.athletid+""===a||i.athlet.toUpperCase().indexOf(a)>-1||i.verein.toUpperCase().indexOf(a)>-1||i.start.toUpperCase().indexOf(a)>-1||i.verein.toUpperCase().indexOf(a)>-1||i.team.toUpperCase().indexOf(a)>-1||r.indexOf(a)>-1)return!0}).length===n.length}static \u0275fac=function(n){return new(n||o)(t.rXU(I.q9),t.rXU(d.nX),t.rXU(j.m))};static \u0275cmp=t.VBU({type:o,selectors:[["app-search-athlet"]],decls:16,vars:4,consts:[["slidingAthletItem",""],["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModelChange","ionInput","ionCancel","ngModel"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],[3,"click","teilnehmer","programm"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","secondary",3,"click"]],template:function(n,i){1&n&&(t.j41(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1)(3,"ion-menu-toggle")(4,"ion-button"),t.nrm(5,"ion-icon",2),t.k0s()()(),t.j41(6,"ion-title")(7,"ion-grid",3)(8,"ion-row")(9,"ion-col")(10,"ion-label"),t.EFF(11,"Suche Turner/-in"),t.k0s()(),t.DNE(12,R,3,1,"ion-col",4),t.k0s()()(),t.DNE(13,E,2,1,"ion-note",5),t.k0s(),t.j41(14,"ion-searchbar",6),t.mxI("ngModelChange",function(a){return t.DH7(i.sMyQuery,a)||(i.sMyQuery=a),a}),t.bIt("ionInput",function(a){return i.reloadList(a)})("ionCancel",function(a){return i.reloadList(a)}),t.k0s()(),t.DNE(15,O,5,4,"ion-content",4)),2&n&&(t.R7$(12),t.Y8G("ngIf",!i.competition),t.R7$(),t.Y8G("ngIf",i.competition),t.R7$(),t.R50("ngModel",i.sMyQuery),t.R7$(),t.Y8G("ngIf",i.competition&&i.startlist&&i.startlist.programme))},dependencies:[m.Sq,m.bT,h.BC,h.vS,s.Jm,s.QW,s.hU,s.W9,s.lO,s.eU,s.iq,s.uz,s.Dg,s.LU,s.CE,s.A7,s.he,s.nf,s.cA,s.JI,s.ln,s.S1,s.Nm,s.Ip,s.w2,s.BC,s.ai,s.Je,s.Gw,T,m.Jj,m.vh]})}return o})();var N=l(5079);const U=[{path:"",component:B}];let x=(()=>{class o{static \u0275fac=function(n){return new(n||o)};static \u0275mod=t.$C({type:o});static \u0275inj=t.G2t({imports:[m.MD,h.YN,s.bv,d.iI.forChild(U),N.h]})}return o})()}}]);