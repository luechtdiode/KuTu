"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[9946],{9946:(G,d,a)=>{a.r(d),a.d(d,{LastResultsPageModule:()=>E});var u=a(9808),h=a(2382),x=a(1631),o=a(7674),v=a(655),p=a(9300),f=a(4004),T=a(8372),b=a(1884),A=a(3099),S=a(3900),C=a(7225),Z=a(7579),L=a(1135),I=a(9646),e=a(7587),y=a(600),M=a(2997);let k=(()=>{class n{constructor(){this.knownKeys=["Rang","Athlet","Jahrgang","Verein","\xf8 Ger\xe4t","Total Punkte"],this.teilnehmerSubResults=""}ngOnInit(){this.teilnehmerSubResults=Object.keys(this.teilnehmer).map(t=>`${t}`).filter(t=>this.knownKeys.indexOf(t)<0).map(t=>`${t}: ${this.teilnehmer[t].Endnote} (${this.teilnehmer[t].Rang}.)`).join(", ")}}return n.\u0275fac=function(t){return new(t||n)},n.\u0275cmp=e.Xpm({type:n,selectors:[["scorelist-item"]],inputs:{teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:17,vars:7,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return i.selected?i.selected.emit(i.teilnehmer):{}}),e.TgZ(1,"ion-avatar",1),e._UZ(2,"img",2),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e.TgZ(5,"small"),e._uU(6),e.qZA(),e._UZ(7,"br"),e._uU(8," \xa0\xa0\xa0\xa0\xa0\xa0"),e.TgZ(9,"small")(10,"em"),e._uU(11),e.qZA()()(),e.TgZ(12,"ion-note",3),e._uU(13),e._UZ(14,"br"),e.TgZ(15,"b"),e._uU(16),e.qZA()()()),2&t&&(e.xp6(4),e.lnq("",i.teilnehmer.Rang,". ",i.teilnehmer.Athlet," (",i.teilnehmer.Jahrgang,")\xa0"),e.xp6(2),e.hij(" - ",i.teilnehmer.Verein,""),e.xp6(5),e.Oqu(i.teilnehmerSubResults),e.xp6(2),e.hij(" Total Punkte ",i.teilnehmer["Total Punkte"],""),e.xp6(3),e.hij("\xf8 Ger\xe4t ",i.teilnehmer["\xf8 Ger\xe4t"],""))},directives:[o.Ie,o.BJ,o.Q$,o.uN],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]}),n})();function R(n,l){if(1&n&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=l.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function P(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(s){return e.CHM(t),e.oxw(2).competition=s}),e.YNc(1,R,3,5,"ion-select-option",9),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function F(n,l){if(1&n&&(e.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,P,2,2,"ion-select",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function w(n,l){if(1&n&&(e.TgZ(0,"ion-note",11),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}function J(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-searchbar",12),e.NdJ("ngModelChange",function(s){return e.CHM(t),e.oxw().sMyQuery=s})("ionInput",function(s){return e.CHM(t),e.oxw().reloadList(s)})("ionCancel",function(s){return e.CHM(t),e.oxw().reloadList(s)}),e.qZA()}if(2&n){const t=e.oxw();e.Q6J("ngModel",t.sMyQuery)}}const N=function(n){return{"gradient-border":n}};function O(n,l){if(1&n&&(e.TgZ(0,"ion-col",14)(1,"div",15),e._UZ(2,"result-display",16),e.qZA()()),2&n){const t=l.$implicit,i=e.oxw(2);e.xp6(1),e.Q6J("ngClass",e.VKq(3,N,i.isNew(t))),e.xp6(1),e.Q6J("item",t)("title",i.getTitle(t))}}function U(n,l){if(1&n&&(e.TgZ(0,"ion-grid",2)(1,"ion-row"),e.YNc(2,O,3,5,"ion-col",13),e.qZA()()),2&n){const t=e.oxw();e.xp6(2),e.Q6J("ngForOf",t.items)}}function Q(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,18)(2,"scorelist-item",19),e.NdJ("click",function(){const r=e.CHM(t).$implicit,c=e.MAs(1);return e.oxw(3).scoreItemTapped(r,c)}),e.qZA(),e.TgZ(3,"ion-item-options",20)(4,"ion-item-option",21),e.NdJ("click",function(){const r=e.CHM(t).$implicit,c=e.MAs(1);return e.oxw(3).followAthlet(r,c)}),e._UZ(5,"ion-icon",22),e._uU(6),e.qZA()()()}if(2&n){const t=l.$implicit;e.xp6(2),e.Q6J("teilnehmer",t),e.xp6(4),e.hij(" Detail-Wertungen ",t.Athlet," ")}}function $(n,l){if(1&n&&(e.TgZ(0,"div")(1,"ion-item-divider"),e._uU(2),e.qZA(),e.YNc(3,Q,7,2,"ion-item-sliding",17),e.qZA()),2&n){const t=l.$implicit;e.xp6(2),e.Oqu(t.title.text),e.xp6(1),e.Q6J("ngForOf",t.rows)}}function z(n,l){if(1&n&&(e.TgZ(0,"ion-list"),e.YNc(1,$,4,2,"div",17),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.filteredScoreList)}}function j(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-footer")(1,"ion-toolbar")(2,"ion-button",23),e.NdJ("click",function(){return e.CHM(t),e.oxw().presentActionSheet()}),e._UZ(3,"ion-icon",24),e.qZA()()()}}let Y=(()=>{class n{constructor(t,i,s){this.navCtrl=t,this.backendService=i,this.actionSheetController=s,this.items=[],this.geraete=[],this.scoreblocks=[],this.sFilteredScoreList=[],this.tMyQueryStream=new Z.x,this.sFilterTask=void 0,this.busy=new L.X(!1),this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(t=>{this.geraete=t||[],this.sortItems()}),this.backendService.newLastResults.pipe((0,p.h)(t=>!!t&&!!t.results)).subscribe(t=>{this.lastItems=this.items.map(i=>i.id*this.geraete.length+i.geraet),this.items=[],Object.keys(t.results).forEach(i=>{this.items.push(t.results[i])}),this.sortItems(),this.scorelistAvailable()&&this.backendService.getScoreList().pipe((0,f.U)(i=>i.title&&i.scoreblocks?i.scoreblocks:[])).subscribe(i=>{this.scoreblocks=i;const s=this.tMyQueryStream.pipe((0,p.h)(r=>!!r&&!!r.target&&!!r.target.value),(0,f.U)(r=>r.target.value),(0,T.b)(1e3),(0,b.x)(),(0,A.B)());s.subscribe(r=>{this.busy.next(!0)}),s.pipe((0,S.w)(this.runQuery(i))).subscribe(r=>{this.sFilteredScoreList=r,this.busy.next(!1)})})})}sortItems(){this.items=this.items.sort((t,i)=>{let s=t.programm.localeCompare(i.programm);return 0===s&&(s=this.geraetOrder(t.geraet)-this.geraetOrder(i.geraet)),s}).filter(t=>void 0!==t.wertung.endnote)}isNew(t){return 0===this.lastItems.filter(i=>i===t.id*this.geraete.length+t.geraet).length}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){this.stationFreezed||(this.backendService.getDurchgaenge(t),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionContainer(){const t={titel:"",datum:new Date,auszeichnung:void 0,auszeichnungendnote:void 0,id:void 0,uuid:void 0,programmId:0};if(!this.backendService.competitions)return t;const i=this.backendService.competitions.filter(s=>s.uuid===this.backendService.competition);return 1===i.length?i[0]:t}competitionName(){const t=this.competitionContainer();return""===t.titel?"":t.titel+", am "+(t.datum+"T").split("T")[0].split("-").reverse().join("-")}geraetOrder(t){return this.geraete?this.geraete.findIndex(s=>s.id===t):0}geraetText(t){if(!this.geraete)return"";const i=this.geraete.filter(s=>s.id===t).map(s=>s.name);return 1===i.length?i[0]:""}getTitle(t){return t.programm+" - "+this.geraetText(t.geraet)}scorelistAvailable(){return!!this.items&&0===this.items.length&&new Date(this.competitionContainer().datum).getTime()<Date.now()}get filteredScoreList(){return this.sFilteredScoreList&&this.sFilteredScoreList.length>0?this.sFilteredScoreList:this.getScoreListItems()}get isBusy(){return this.busy}runQuery(t){return i=>{const s=i.trim();let r=[];return s&&t&&t.forEach(c=>{const g=this.filter(s),_=c.rows.filter(m=>g(m,c));if(_.length>0){const m={title:c.title,rows:_};r=[...r,m]}}),(0,I.of)(r)}}filter(t){const i=t.toUpperCase().split(" ");return(s,r)=>i.filter(c=>{if(s.Athlet.toUpperCase().indexOf(c)>-1||s.Verein.toUpperCase().indexOf(c)>-1||s.Jahrgang.toUpperCase().indexOf(c)>-1||r.title.text.toUpperCase().replace("."," ").replace(","," ").split(" ").indexOf(c)>-1)return!0}).length===i.length}reloadList(t){this.tMyQueryStream.next(t)}makeScoreListLink(){const t=this.competitionContainer();return`${C.AC}api/scores/${t.uuid}/query?groupby=Kategorie:Geschlecht&html`}getScoreListItems(){return this.scoreblocks}scoreItemTapped(t,i){i.getOpenAmount().then(s=>{s>0?i.close():i.open("end")})}followAthlet(t,i){i.close(),this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${t.athletID}`)}open(){window.open(this.makeScoreListLink(),"_blank")}isShareAvailable(){return!!navigator&&!!navigator.share}share(){let t="GeTu";const s=this.competitionContainer();""!==s.titel&&(t={1:"Athletik",11:"KuTu",20:"GeTu",31:"KuTu"}[s.programmId]);let r=`${this.competitionName()} #${t}-${s.titel.replace(","," ").split(" ").join("_")}`;this.isShareAvailable()&&navigator.share({title:`${t} Rangliste`,text:r,url:this.makeScoreListLink()}).then(function(){console.log("Article shared")}).catch(function(c){console.log(c.message)})}presentActionSheet(){return(0,v.mG)(this,void 0,void 0,function*(){let t=[{text:"Rangliste \xf6ffnen ...",icon:"open",handler:()=>{this.open()}}];this.isShareAvailable()&&(t=[...t,{text:"Teilen / Share ...",icon:"share",handler:()=>{this.share()}}]);let i={header:"Aktionen",cssClass:"my-actionsheet-class",buttons:t};yield(yield this.actionSheetController.create(i)).present()})}}return n.\u0275fac=function(t){return new(t||n)(e.Y36(o.SH),e.Y36(y.v),e.Y36(o.BX))},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-last-results"]],decls:19,vars:6,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel",4,"ngIf"],["no-padding","",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start"],[3,"ngClass"],[3,"item","title"],[4,"ngFor","ngForOf"],["slidingAthletScoreItem",""],[3,"teilnehmer","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["size","large","expand","block","color","primary",3,"click"],["slot","start","name","list"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),e._UZ(5,"ion-icon",1),e.qZA()()(),e.TgZ(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),e._uU(11,"Aktuelle Resultate"),e.qZA()(),e.YNc(12,F,5,1,"ion-col",3),e.qZA()()(),e.YNc(13,w,2,1,"ion-note",4),e.qZA(),e.YNc(14,J,1,1,"ion-searchbar",5),e.qZA(),e.TgZ(15,"ion-content"),e.YNc(16,U,3,1,"ion-grid",6),e.YNc(17,z,2,1,"ion-list",3),e.qZA(),e.YNc(18,j,4,0,"ion-footer",3)),2&t&&(e.xp6(12),e.Q6J("ngIf",!i.competition),e.xp6(1),e.Q6J("ngIf",i.competition),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(2),e.Q6J("ngIf",i.items.length>0),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()))},directives:[o.Gu,o.sr,o.Sm,o.zc,o.YG,o.gu,o.wd,o.jY,o.Nd,o.wI,o.Q$,u.O5,o.Ie,o.t9,o.QI,h.JJ,h.On,u.sg,o.n0,o.uN,o.VI,o.j9,o.W2,u.mk,M.G,o.q_,o.rH,o.td,k,o.IK,o.u8,o.fr],pipes:[u.uU],styles:[""]}),n})();var q=a(5051);const B=[{path:"",component:Y}];let E=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[[u.ez,h.u5,o.Pc,x.Bz.forChild(B),q.K]]}),n})()}}]);