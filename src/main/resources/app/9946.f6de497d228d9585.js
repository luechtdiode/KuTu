"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[9946],{9946:(he,_,c)=>{c.r(_),c.d(_,{LastResultsPageModule:()=>ce});var m=c(6895),d=c(433),A=c(5472),a=c(502),S=c(5861),f=c(4004),v=c(9300),Z=c(8372),C=c(1884),I=c(3099),y=c(3900),L=c(7225),w=c(7579),M=c(1135),P=c(9646),x=c(2997),e=c(8274),R=c(600);function J(n,o){1&n&&(e.TgZ(0,"ion-avatar",4),e._UZ(1,"img",5),e.qZA())}function O(n,o){1&n&&(e.TgZ(0,"ion-avatar",4),e._UZ(1,"img",6),e.qZA())}function U(n,o){if(1&n&&(e.TgZ(0,"li")(1,"small")(2,"em"),e._uU(3),e.qZA()()()),2&n){const t=o.$implicit;e.xp6(3),e.Oqu(t)}}function k(n,o){if(1&n&&(e.TgZ(0,"ion-label"),e._uU(1),e._UZ(2,"br"),e._uU(3," \xa0\xa0\xa0\xa0\xa0\xa0"),e.TgZ(4,"small")(5,"em"),e._uU(6),e.qZA()(),e._UZ(7,"br"),e.TgZ(8,"ul"),e.YNc(9,U,4,1,"li",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(1),e.AsE("",t.teilnehmer.Rang,". ",t.teilnehmer["Team/Athlet"],""),e.xp6(5),e.Oqu(t.teilnehmerSubResults),e.xp6(3),e.Q6J("ngForOf",t.teamTeilnehmerSubResults)}}function N(n,o){if(1&n&&(e.TgZ(0,"ion-label"),e._uU(1),e.TgZ(2,"small"),e._uU(3),e.qZA(),e._UZ(4,"br"),e._uU(5," \xa0\xa0\xa0\xa0\xa0\xa0"),e.TgZ(6,"small")(7,"em"),e._uU(8),e.qZA()()()),2&n){const t=e.oxw();e.xp6(1),e.lnq("",t.teilnehmer.Rang,". ",t.teilnehmer.Athlet," (",t.teilnehmer.Jahrgang,")\xa0"),e.xp6(2),e.hij(" - ",t.teilnehmer.Verein,""),e.xp6(5),e.Oqu(t.teilnehmerSubResults)}}function Q(n,o){if(1&n&&(e.TgZ(0,"em"),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij("\xf8 Ger\xe4t ",t.teilnehmer["\xf8 Ger\xe4t"],"")}}function F(n,o){if(1&n&&(e.TgZ(0,"em"),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.AsE("Total D/E ",t.teilnehmer["Total D"],"/",t.teilnehmer["Total E"],"")}}function q(n,o){if(1&n&&(e.TgZ(0,"em"),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.AsE("Total A/B ",t.teilnehmer["Total A"],"/",t.teilnehmer["Total B"],"")}}let $=(()=>{class n{constructor(){this.teilnehmerSubResults="",this.teamTeilnehmerSubResults=[]}ngOnInit(){this.teilnehmerSubResults=T(this.teilnehmer),this.teamTeilnehmerSubResults=0==this.teilnehmer.rows?.length?[]:this.teilnehmer.rows.map(G)}}return n.\u0275fac=function(t){return new(t||n)},n.\u0275cmp=e.Xpm({type:n,selectors:[["scorelist-item"]],inputs:{teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:12,vars:8,consts:[[3,"click"],["slot","start",4,"ngIf"],[4,"ngIf"],["slot","end"],["slot","start"],["src","assets/imgs/athlete.png"],["src","assets/imgs/verein.png"],[4,"ngFor","ngForOf"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return i.selected?i.selected.emit(i.teilnehmer):{}}),e.YNc(1,J,2,0,"ion-avatar",1),e.YNc(2,O,2,0,"ion-avatar",1),e.YNc(3,k,10,4,"ion-label",2),e.YNc(4,N,9,5,"ion-label",2),e.TgZ(5,"ion-note",3)(6,"b"),e._uU(7),e.qZA(),e._UZ(8,"br"),e.YNc(9,Q,2,1,"em",2),e.YNc(10,F,2,2,"em",2),e.YNc(11,q,2,2,"em",2),e.qZA()()),2&t&&(e.xp6(1),e.Q6J("ngIf",i.teilnehmer.Athlet),e.xp6(1),e.Q6J("ngIf",i.teamTeilnehmerSubResults.length>0),e.xp6(1),e.Q6J("ngIf",i.teamTeilnehmerSubResults.length>0),e.xp6(1),e.Q6J("ngIf",i.teilnehmer.Athlet),e.xp6(3),e.hij("Total Punkte ",i.teilnehmer["Total Punkte"],""),e.xp6(2),e.Q6J("ngIf",i.teilnehmer["\xf8 Ger\xe4t"]),e.xp6(1),e.Q6J("ngIf",i.teilnehmer["Total D"]),e.xp6(1),e.Q6J("ngIf",i.teilnehmer["Total A"]))},dependencies:[m.sg,m.O5,a.BJ,a.Ie,a.Q$,a.uN],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]}),n})();const Y=["athletID","rows","Rang","Athlet","Team","Team/Athlet","Jahrgang","Verein","K","\xf8 Ger\xe4t","Total D","Total E","Total A","Total B","Total Punkte"];function G(n){return`${n.Team} (${n.K}): ${T(n)}`}function T(n){return Object.keys(n).map(o=>`${o}`).filter(o=>Y.indexOf(o)<0).map(o=>`${o}: ${n[o].Endnote} (${n[o].Rang})`).join(", ")}function z(n,o){if(1&n&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=o.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function B(n,o){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(s){e.CHM(t);const l=e.oxw(2);return e.KtG(l.competition=s)}),e.YNc(1,z,3,5,"ion-select-option",9),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function E(n,o){if(1&n&&(e.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,B,2,2,"ion-select",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function j(n,o){if(1&n&&(e.TgZ(0,"ion-note",11),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}function K(n,o){if(1&n){const t=e.EpF();e.TgZ(0,"ion-searchbar",12),e.NdJ("ngModelChange",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.sMyQuery=s)})("ionInput",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.reloadList(s))})("ionCancel",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.reloadList(s))}),e.qZA()}if(2&n){const t=e.oxw();e.Q6J("ngModel",t.sMyQuery)}}const b=function(n){return{"gradient-border":n}};function D(n,o){if(1&n&&(e.TgZ(0,"ion-col",15)(1,"div",16),e._UZ(2,"result-display",17),e.qZA()()),2&n){const t=o.$implicit,i=e.oxw(3);e.uIk("size-xl",i.getMaxColumnSpec()),e.xp6(1),e.Q6J("ngClass",e.VKq(5,b,i.isNew(t))),e.xp6(1),e.Q6J("item",t)("title",i.getTitle(t))("groupedBy",i.groupBy.PROGRAMM)}}function H(n,o){if(1&n&&(e.TgZ(0,"ion-row"),e.YNc(1,D,3,7,"ion-col",14),e.qZA()),2&n){const t=o.$implicit,i=e.oxw(2);e.xp6(1),e.Q6J("ngForOf",i.getWertungen(t))}}function W(n,o){if(1&n&&(e.TgZ(0,"ion-grid",2),e.YNc(1,H,2,1,"ion-row",13),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.getProgramme())}}function V(n,o){if(1&n&&(e.TgZ(0,"ion-col",19)(1,"div",16),e._UZ(2,"result-display",17),e.qZA()()),2&n){const t=o.$implicit,i=e.oxw(3);e.uIk("size-xl",i.getMaxColumnSpec()),e.xp6(1),e.Q6J("ngClass",e.VKq(5,b,i.isNew(t))),e.xp6(1),e.Q6J("item",t)("title",i.getTitle(t))("groupedBy",i.groupBy.PROGRAMM)}}function X(n,o){if(1&n&&(e.TgZ(0,"ion-row"),e.YNc(1,V,3,7,"ion-col",18),e.qZA()),2&n){const t=o.$implicit,i=e.oxw(2);e.xp6(1),e.Q6J("ngForOf",i.getWertungen(t))}}function ee(n,o){if(1&n&&(e.TgZ(0,"ion-grid",2),e.YNc(1,X,2,1,"ion-row",13),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.getProgramme())}}function te(n,o){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-option",24),e.NdJ("click",function(){e.CHM(t);const s=e.oxw().$implicit,l=e.MAs(1),r=e.oxw(3);return e.KtG(r.followAthlet(s,l))}),e._UZ(1,"ion-icon",25),e._uU(2),e.qZA()}if(2&n){const t=e.oxw().$implicit;e.xp6(2),e.hij(" Detail-Wertungen ",t.Athlet," ")}}function ie(n,o){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,20)(2,"scorelist-item",21),e.NdJ("click",function(){const l=e.CHM(t).$implicit,r=e.MAs(1),u=e.oxw(3);return e.KtG(u.scoreItemTapped(l,r))}),e.qZA(),e.TgZ(3,"ion-item-options",22),e.YNc(4,te,3,1,"ion-item-option",23),e.qZA()()}if(2&n){const t=o.$implicit;e.xp6(2),e.Q6J("teilnehmer",t),e.xp6(2),e.Q6J("ngIf",t.athletID)}}function ne(n,o){if(1&n&&(e.TgZ(0,"div")(1,"ion-item-divider"),e._uU(2),e.qZA(),e.YNc(3,ie,5,2,"ion-item-sliding",13),e.qZA()),2&n){const t=o.$implicit;e.xp6(2),e.Oqu(t.title.text),e.xp6(1),e.Q6J("ngForOf",t.rows)}}function se(n,o){if(1&n&&(e.TgZ(0,"ion-list"),e.YNc(1,ne,4,2,"div",13),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.filteredScoreList)}}function oe(n,o){if(1&n){const t=e.EpF();e.TgZ(0,"ion-footer")(1,"ion-toolbar")(2,"ion-button",26),e.NdJ("click",function(){e.CHM(t);const s=e.oxw();return e.KtG(s.presentActionSheet())}),e._UZ(3,"ion-icon",27),e.qZA()()()}}let re=(()=>{class n{constructor(t,i,s){this.navCtrl=t,this.backendService=i,this.actionSheetController=s,this.groupBy=x.X,this.items=[],this.geraete=[],this.scorelinks=[],this.defaultPath=void 0,this.scoreblocks=[],this.sFilteredScoreList=[],this.tMyQueryStream=new w.x,this.sFilterTask=void 0,this.busy=new M.X(!1),this.subscriptions=[],this._title="Aktuelle Resultate",this.backendService.competitions||this.backendService.getCompetitions(),this.backendService.durchgangStarted.pipe((0,f.U)(l=>l.filter(r=>r.wettkampfUUID===this.backendService.competition).length>0)).subscribe(l=>{this.durchgangopen=l})}ngOnDestroy(){this.subscriptions.forEach(t=>t.unsubscribe())}ngOnInit(){this.subscriptions.push(this.backendService.competitionSubject.subscribe(t=>{this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}),this.subscriptions.push(this.backendService.newLastResults.subscribe(i=>{this.lastItems=this.items.map(s=>s.id*this.geraete.length+s.geraet),this.items=[],!!i&&!!i.results&&Object.keys(i.results).forEach(s=>{this.items.push(i.results[s])}),this.sortItems(),this.scorelistAvailable()?this.backendService.getScoreLists().subscribe(s=>{const l=this.competitionContainer(),r=`/api/scores/${l.uuid}/query?groupby=Kategorie:Geschlecht`;if(s){const h=Object.values(s).filter(p=>"Zwischenresultate"!=p.name).sort((p,ge)=>p.name.localeCompare(ge.name)),g={name:"Generische Rangliste",published:!0,"published-date":"","scores-href":r,"scores-query":r},ue={name:"Generische Team-Rangliste",published:!0,"published-date":"","scores-href":r+"&kind=Teamrangliste","scores-query":r+"&kind=Teamrangliste"};this.scorelinks=l.teamrule?.trim.length>0?[...h,ue,g]:[...h,g];const me=this.scorelinks.filter(p=>""+p.published=="true");this.refreshScoreList(me[0])}}):this.title="Aktuelle Resultate"}))}))}get title(){return this._title}set title(t){this._title=t}refreshScoreList(t){let i=t["scores-href"];t.published||(i=t["scores-query"]),i.startsWith("/")&&(i=i.substring(1)),i=i.replace("html",""),this.title=t.name,this.defaultPath=i,this.backendService.getScoreList(this.defaultPath).pipe((0,f.U)(s=>s.title&&s.scoreblocks?s.scoreblocks:[])).subscribe(s=>{this.scoreblocks=s;const l=this.tMyQueryStream.pipe((0,v.h)(r=>!!r&&!!r.target&&!!r.target.value),(0,f.U)(r=>r.target.value),(0,Z.b)(1e3),(0,C.x)(),(0,I.B)());l.subscribe(()=>{this.busy.next(!0)}),l.pipe((0,y.w)(this.runQuery(s))).subscribe(r=>{this.sFilteredScoreList=r,this.busy.next(!1)})})}sortItems(){this.items=this.items.filter(i=>void 0!==i.wertung.endnote).sort((i,s)=>{let l=i.programm.localeCompare(s.programm);return 0===l&&(l=this.geraetOrder(i.geraet)-this.geraetOrder(s.geraet)),l})}isNew(t){return 0===this.lastItems.filter(i=>i===t.id*this.geraete.length+t.geraet).length}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){this.stationFreezed||(this.backendService.getDurchgaenge(t),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionContainer(){const t={titel:"",datum:new Date,auszeichnung:void 0,auszeichnungendnote:void 0,id:void 0,uuid:void 0,programmId:0,teamrule:""};if(!this.backendService.competitions)return t;const i=this.backendService.competitions.filter(s=>s.uuid===this.backendService.competition);return 1===i.length?i[0]:t}competitionName(){const t=this.competitionContainer();return""===t.titel?"":t.titel+", am "+(t.datum+"T").split("T")[0].split("-").reverse().join("-")}geraetOrder(t){return this.geraete?this.geraete.findIndex(i=>i.id===t):0}geraetText(t){if(!this.geraete)return"";const i=this.geraete.filter(s=>s.id===t).map(s=>s.name);return 1===i.length?i[0]:""}getColumnSpec(){return this.geraete?.length||0}getMaxColumnSpec(){return Math.min(12,Math.max(1,Math.floor(12/this.geraete.length+.5)))}getTitle(t){return t.programm+" - "+this.geraetText(t.geraet)}onlyUnique(t,i,s){return s.indexOf(t)===i}getProgramme(){return this.items.map(t=>t.programm).filter(this.onlyUnique)}getWertungen(t){return this.items.filter(i=>i.programm===t)}scorelistAvailable(){return!this.durchgangopen&&0===this.items?.length&&new Date(this.competitionContainer().datum).getTime()<new Date(Date.now()-864e5).getTime()}get filteredScoreList(){return this.sFilteredScoreList&&this.sFilteredScoreList.length>0?this.sFilteredScoreList:this.getScoreListItems()}get isBusy(){return this.busy}runQuery(t){return i=>{const s=i.trim();let l=[];return s&&t&&t.forEach(r=>{const u=this.filter(s),h=r.rows.filter(g=>u(g,r));if(h.length>0){const g={title:r.title,rows:h};l=[...l,g]}}),(0,P.of)(l)}}filter(t){const i=t.toUpperCase().split(" ");return(s,l)=>i.filter(r=>[s,...s.rows].find(u=>{if(u.Athlet?.toUpperCase().indexOf(r)>-1||u["Team/Athlet"]?.toUpperCase().indexOf(r)>-1||u.Team?.toUpperCase().indexOf(r)>-1||u.Verein?.toUpperCase().indexOf(r)>-1||u.Jahrgang?.toUpperCase().indexOf(r)>-1||l.title.text.toUpperCase().replace("."," ").replace(","," ").split(" ").indexOf(r)>-1)return!0})).length===i.length}reloadList(t){this.tMyQueryStream.next(t)}makeGenericScoreListLink(){return`${L.AC}${this.defaultPath}&html`}getScoreListItems(){return this.scoreblocks}scoreItemTapped(t,i){i.getOpenAmount().then(s=>{s>0?i.close():i.open("end")})}followAthlet(t,i){i.close(),t.athletID&&this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${t.athletID}`)}open(){window.open(this.makeGenericScoreListLink(),"_blank")}isShareAvailable(){return!!navigator&&!!navigator.share}share(){let t="GeTu";const s=this.competitionContainer();""!==s.titel&&(t={1:"Athletik",11:"KuTu",20:"GeTu",31:"KuTu"}[s.programmId]);let l=`${this.competitionName()} #${t}-${s.titel.replace(","," ").split(" ").join("_")}`;this.isShareAvailable()&&navigator.share({title:`${t} Rangliste`,text:l,url:this.makeGenericScoreListLink()}).then(function(){console.log("Article shared")}).catch(function(r){console.log(r.message)})}presentActionSheet(){var t=this;return(0,S.Z)(function*(){let i=[...t.scorelinks.map(r=>""+r.published=="true"?{text:`${r.name} anzeigen ...`,icon:"document",handler:()=>{t.refreshScoreList(r)}}:{text:`${r.name} (unver\xf6ffentlicht)`,icon:"today-outline",handler:()=>{t.refreshScoreList(r)}}),{text:"Rangliste \xf6ffnen ...",icon:"open",handler:()=>{t.open()}}];t.isShareAvailable()&&(i=[...i,{text:"Teilen / Share ...",icon:"share",handler:()=>{t.share()}}]);let s={header:"Aktionen",cssClass:"my-actionsheet-class",buttons:i};yield(yield t.actionSheetController.create(s)).present()})()}}return n.\u0275fac=function(t){return new(t||n)(e.Y36(a.SH),e.Y36(R.v),e.Y36(a.BX))},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-last-results"]],decls:20,vars:8,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel",4,"ngIf"],["no-padding","",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],[4,"ngFor","ngForOf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","3",1,"align-self-start"],[3,"ngClass"],[3,"item","title","groupedBy"],["class","align-self-start","size-xs","12","size-sm","6","size-md","4","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-sm","6","size-md","4","size-lg","3",1,"align-self-start"],["slidingAthletScoreItem",""],[3,"teilnehmer","click"],["side","end"],["color","primary",3,"click",4,"ngIf"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["size","large","expand","block","color","primary",3,"click"],["slot","start","name","list"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),e._UZ(5,"ion-icon",1),e.qZA()()(),e.TgZ(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),e._uU(11),e.qZA()(),e.YNc(12,E,5,1,"ion-col",3),e.qZA()()(),e.YNc(13,j,2,1,"ion-note",4),e.qZA(),e.YNc(14,K,1,1,"ion-searchbar",5),e.qZA(),e.TgZ(15,"ion-content"),e.YNc(16,W,2,1,"ion-grid",6),e.YNc(17,ee,2,1,"ion-grid",6),e.YNc(18,se,2,1,"ion-list",3),e.qZA(),e.YNc(19,oe,4,0,"ion-footer",3)),2&t&&(e.xp6(11),e.Oqu(i.title),e.xp6(1),e.Q6J("ngIf",!i.competition),e.xp6(1),e.Q6J("ngIf",i.competition),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(2),e.Q6J("ngIf",i.items.length>0&&6===i.getColumnSpec()),e.xp6(1),e.Q6J("ngIf",i.items.length>0&&6!==i.getColumnSpec()),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()))},dependencies:[m.mk,m.sg,m.O5,d.JJ,d.On,a.YG,a.Sm,a.wI,a.W2,a.fr,a.jY,a.Gu,a.gu,a.Ie,a.rH,a.u8,a.IK,a.td,a.Q$,a.q_,a.zc,a.uN,a.Nd,a.VI,a.t9,a.n0,a.wd,a.sr,a.QI,a.j9,x.G,$,m.uU]}),n})();var le=c(5051);const ae=[{path:"",component:re}];let ce=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[m.ez,d.u5,a.Pc,A.Bz.forChild(ae),le.K]}),n})()}}]);