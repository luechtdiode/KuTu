"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8910],{8910:(B,g,c)=>{c.r(g),c.d(g,{SearchAthletPageModule:()=>j});var h=c(6814),u=c(95),d=c(335),r=c(3582),_=c(8645),v=c(5619),S=c(2096),A=c(2181),T=c(7398),x=c(3620),Z=c(3997),b=c(3020),C=c(4664),t=c(2029),y=c(4414),M=c(9253);let U=(()=>{class o{programm;teilnehmer;selected;constructor(){}ngOnInit(){}static \u0275fac=function(n){return new(n||o)};static \u0275cmp=t.Xpm({type:o,selectors:[["startlist-item"]],inputs:{programm:"programm",teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:16,vars:5,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(n,i){1&n&&(t.TgZ(0,"ion-item",0),t.NdJ("click",function(){return i.selected?i.selected.emit(i.teilnehmer):{}}),t.TgZ(1,"ion-avatar",1),t._UZ(2,"img",2),t.qZA(),t.TgZ(3,"ion-label"),t._uU(4),t.TgZ(5,"small"),t._uU(6),t.qZA(),t._UZ(7,"br"),t.TgZ(8,"small")(9,"em"),t._uU(10),t.qZA()()(),t.TgZ(11,"ion-note",3),t._uU(12," Startger\xe4t:"),t._UZ(13,"br"),t.TgZ(14,"b"),t._uU(15),t.qZA()()()),2&n&&(t.xp6(4),t.hij("",i.teilnehmer.athlet,"\xa0"),t.xp6(2),t.Oqu(i.teilnehmer.verein+", "+i.programm.programm),t.xp6(4),t.AsE("",i.teilnehmer.durchgang," ",i.teilnehmer.team,""),t.xp6(5),t.Oqu(i.teilnehmer.start))},dependencies:[r.BJ,r.Ie,r.Q$,r.uN],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]})}return o})();function Q(o,l){if(1&o&&(t.TgZ(0,"ion-select-option",9),t._uU(1),t.ALo(2,"date"),t.qZA()),2&o){const e=l.$implicit;t.Q6J("value",e.uuid),t.xp6(1),t.hij(" ",e.titel+" "+t.xi3(2,2,e.datum,"dd-MM-yy"),"")}}function O(o,l){if(1&o){const e=t.EpF();t.TgZ(0,"ion-select",7),t.NdJ("ngModelChange",function(i){t.CHM(e);const s=t.oxw(2);return t.KtG(s.competition=i)}),t.YNc(1,Q,3,5,"ion-select-option",8),t.qZA()}if(2&o){const e=t.oxw(2);t.Q6J("ngModel",e.competition),t.xp6(1),t.Q6J("ngForOf",e.getCompetitions())}}function I(o,l){if(1&o&&(t.TgZ(0,"ion-col")(1,"ion-item"),t.YNc(2,O,2,2,"ion-select",6),t.qZA()()),2&o){const e=t.oxw();t.xp6(2),t.Q6J("ngIf",e.getCompetitions().length>0)}}function k(o,l){if(1&o&&(t.TgZ(0,"ion-note",10),t._uU(1),t.qZA()),2&o){const e=t.oxw();t.xp6(1),t.hij(" ",e.competitionName()," ")}}function F(o,l){1&o&&(t.TgZ(0,"ion-item")(1,"ion-label"),t._uU(2,"loading ..."),t.qZA(),t._UZ(3,"ion-spinner"),t.qZA())}function J(o,l){if(1&o){const e=t.EpF();t.TgZ(0,"ion-item-sliding",null,12)(2,"startlist-item",13),t.NdJ("click",function(){const s=t.CHM(e).$implicit,a=t.MAs(1),m=t.oxw(3);return t.KtG(m.itemTapped(s,a))}),t.qZA(),t.TgZ(3,"ion-item-options",14)(4,"ion-item-option",15),t.NdJ("click",function(){const s=t.CHM(e).$implicit,a=t.MAs(1),m=t.oxw(3);return t.KtG(m.followAthlet(s,a))}),t._UZ(5,"ion-icon",16),t._uU(6),t.qZA(),t.TgZ(7,"ion-item-option",17),t.NdJ("click",function(){const s=t.CHM(e).$implicit,a=t.MAs(1),m=t.oxw(3);return t.KtG(m.followRiege(s,a))}),t._UZ(8,"ion-icon",16),t._uU(9," Riege verfolgen "),t.qZA()()()}if(2&o){const e=l.$implicit,n=t.oxw().$implicit;t.xp6(2),t.Q6J("teilnehmer",e)("programm",n),t.xp6(4),t.hij(" ",e.athlet," verfolgen ")}}function P(o,l){if(1&o&&(t.TgZ(0,"div")(1,"ion-item-divider"),t._uU(2),t.qZA(),t.YNc(3,J,10,3,"ion-item-sliding",11),t.qZA()),2&o){const e=l.$implicit;t.xp6(2),t.Oqu(e.programm),t.xp6(1),t.Q6J("ngForOf",e.teilnehmer)}}function w(o,l){if(1&o&&(t.TgZ(0,"ion-content")(1,"ion-list"),t.YNc(2,F,4,0,"ion-item",3),t.ALo(3,"async"),t.YNc(4,P,4,2,"div",11),t.qZA()()),2&o){const e=t.oxw();t.xp6(2),t.Q6J("ngIf",t.lcZ(3,2,e.isBusy)),t.xp6(2),t.Q6J("ngForOf",e.filteredStartList.programme)}}let N=(()=>{class o{navCtrl;route;backendService;sStartList;sFilteredStartList;sMyQuery;tMyQueryStream=new _.x;sFilterTask=void 0;busy=new v.X(!1);constructor(e,n,i){this.navCtrl=e,this.route=n,this.backendService=i,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const e=this.route.snapshot.paramMap.get("wkId");e&&(this.competition=e)}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){(!this.startlist||e!==this.backendService.competition)&&(this.busy.next(!0),this.startlist={},this.backendService.getDurchgaenge(e),this.backendService.loadStartlist(void 0).subscribe(n=>{this.startlist=n,this.busy.next(!1);const i=this.tMyQueryStream.pipe((0,A.h)(s=>!!s&&!!s.target&&!!s.target.value),(0,T.U)(s=>s.target.value),(0,x.b)(1e3),(0,Z.x)(),(0,b.B)());i.subscribe(s=>{this.busy.next(!0)}),i.pipe((0,C.w)(this.runQuery(n))).subscribe(s=>{this.sFilteredStartList=s,this.busy.next(!1)})}))}get competition(){return this.backendService.competition||""}set startlist(e){this.sStartList=e,this.reloadList(this.sMyQuery)}get startlist(){return this.sStartList}get filteredStartList(){return this.sFilteredStartList||{programme:[]}}get isBusy(){return this.busy}runQuery(e){return n=>{const i=n.trim();let s;return s={programme:[]},i&&e&&e.programme.forEach(a=>{const m=this.filter(i),f=a.teilnehmer.filter(p=>m(p,a.programm));f.length>0&&(s=Object.assign({},e,{programme:[...s.programme,{programm:a.programm,teilnehmer:f}]}))}),(0,S.of)(s)}}reloadList(e){this.tMyQueryStream.next(e)}itemTapped(e,n){n.getOpenAmount().then(i=>{i>0?n.close():n.open("end")})}followAthlet(e,n){n.close(),this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${e.athletid}`)}followRiege(e,n){n.close(),this.backendService.getGeraete(this.backendService.competition,e.durchgang).subscribe(i=>{this.backendService.getSteps(this.backendService.competition,e.durchgang,i.find(s=>s.name===e.start).id).subscribe(s=>{this.navCtrl.navigateForward("station")})})}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const e=this.backendService.competitions.filter(n=>n.uuid===this.backendService.competition).map(n=>n.titel+", am "+(n.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===e.length?(this.startlist||(this.competition=this.backendService.competition),e[0]):""}filter(e){const n=e.toUpperCase().split(" ");return(i,s)=>n.filter(a=>{if(i.athletid+""===a||i.athlet.toUpperCase().indexOf(a)>-1||i.verein.toUpperCase().indexOf(a)>-1||i.start.toUpperCase().indexOf(a)>-1||i.verein.toUpperCase().indexOf(a)>-1||i.team.toUpperCase().indexOf(a)>-1||s.indexOf(a)>-1)return!0}).length===n.length}static \u0275fac=function(n){return new(n||o)(t.Y36(y.SH),t.Y36(d.gz),t.Y36(M.v))};static \u0275cmp=t.Xpm({type:o,selectors:[["app-search-athlet"]],decls:16,vars:4,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingAthletItem",""],[3,"teilnehmer","programm","click"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","secondary",3,"click"]],template:function(n,i){1&n&&(t.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),t._UZ(5,"ion-icon",1),t.qZA()()(),t.TgZ(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),t._uU(11,"Suche Turner/-in"),t.qZA()(),t.YNc(12,I,3,1,"ion-col",3),t.qZA()()(),t.YNc(13,k,2,1,"ion-note",4),t.qZA(),t.TgZ(14,"ion-searchbar",5),t.NdJ("ngModelChange",function(a){return i.sMyQuery=a})("ionInput",function(a){return i.reloadList(a)})("ionCancel",function(a){return i.reloadList(a)}),t.qZA()(),t.YNc(15,w,5,4,"ion-content",3)),2&n&&(t.xp6(12),t.Q6J("ngIf",!i.competition),t.xp6(1),t.Q6J("ngIf",i.competition),t.xp6(1),t.Q6J("ngModel",i.sMyQuery),t.xp6(1),t.Q6J("ngIf",i.competition&&i.startlist&&i.startlist.programme))},dependencies:[h.sg,h.O5,u.JJ,u.On,r.YG,r.Sm,r.wI,r.W2,r.jY,r.Gu,r.gu,r.Ie,r.rH,r.u8,r.IK,r.td,r.Q$,r.q_,r.zc,r.uN,r.Nd,r.VI,r.t9,r.n0,r.PQ,r.wd,r.sr,r.QI,r.j9,U,h.Ov,h.uU]})}return o})();var L=c(3573);const Y=[{path:"",component:N}];let j=(()=>{class o{static \u0275fac=function(n){return new(n||o)};static \u0275mod=t.oAB({type:o});static \u0275inj=t.cJS({imports:[h.ez,u.u5,r.Pc,d.Bz.forChild(Y),L.K]})}return o})()}}]);