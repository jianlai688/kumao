<template>
  <div class="musician-console">
    <section class="artist-band" aria-labelledby="artist-title">
      <div class="artist-avatar">
        <img v-if="musician?.avatar" :src="musician.avatar" :alt="`${musician?.stageName || '音乐人'}头像`" @error="onAvatarError" />
        <span v-else aria-hidden="true">{{ (musician?.stageName || '音').slice(0, 1) }}</span>
      </div>
      <div class="artist-copy">
        <h1 id="artist-title">{{ musician?.stageName || '音乐人工作台' }}</h1>
        <div class="artist-meta">
          <span :class="['state-pill', musician?.status === 'APPROVED' ? 'is-positive' : 'is-warning']">
            {{ musician?.status === 'APPROVED' ? '已认证音乐人' : '认证审核中' }}
          </span>
          <span v-if="musician?.style">{{ musician.style }}</span>
          <span v-if="musician">{{ formatNum(musician.fansCount) }} 粉丝</span>
          <span v-if="works.length">{{ works.length }} 首作品</span>
        </div>
      </div>
      <div class="artist-actions">
        <button class="button button-quiet" type="button" @click="goToHomepage">
          <el-icon><User /></el-icon>
          音乐人主页
        </button>
        <button
          class="button button-primary"
          type="button"
          :disabled="musician?.status !== 'APPROVED'"
          :title="musician?.status === 'APPROVED' ? '上传原创作品' : '认证通过后才能上传作品'"
          @click="openWorkEditor()"
        >
          <el-icon><Upload /></el-icon>
          上传作品
        </button>
      </div>
    </section>

    <div v-if="profileError" class="inline-alert" role="alert">
      <span>{{ profileError }}</span>
      <button type="button" @click="loadProfile">重新加载</button>
    </div>

    <div v-if="musician && musician.status !== 'APPROVED'" class="inline-alert is-warning" role="status">
      <span>音乐人认证尚未通过，作品上传与数据功能暂不可用。</span>
      <router-link to="/apply/musician">前往认证</router-link>
    </div>

    <nav class="console-tabs" role="tablist" aria-label="音乐人工作台功能">
      <button
        v-for="(item, index) in tabs"
        :id="`musician-tab-${item.name}`"
        :key="item.name"
        class="console-tab"
        :class="{ active: tab === item.name }"
        role="tab"
        type="button"
        :aria-selected="tab === item.name"
        :aria-controls="`musician-panel-${item.name}`"
        :tabindex="tab === item.name ? 0 : -1"
        @click="switchTab(item.name)"
        @keydown="onTabKeydown($event, index)"
      >
        {{ item.label }}
      </button>
    </nav>

    <div
      v-if="tab === 'works'"
      id="musician-panel-works"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-works"
      tabindex="0"
    >
      <div class="section-head">
        <div>
          <h2>作品管理</h2>
          <p>管理原创歌曲的审核、上架与数据表现。</p>
        </div>
        <span class="section-note">{{ works.length }} 首作品<span v-if="pendingCount"> · {{ pendingCount }} 首待审核</span></span>
      </div>

      <div v-if="workDraft" class="draft-banner" role="status">
        <div class="draft-copy">
          <span class="draft-mark">本机草稿</span>
          <strong>{{ workDraft.form?.name || '未命名作品' }}</strong>
          <span>{{ formatDateTime(workDraft.savedAt) }} 保存</span>
        </div>
        <div class="draft-actions">
          <button type="button" class="text-button" @click="resumeWorkDraft">继续编辑</button>
          <button type="button" class="text-button is-danger" @click="discardWorkDraft">删除草稿</button>
        </div>
      </div>

      <div class="work-toolbar">
        <label class="search-box">
          <el-icon aria-hidden="true"><Search /></el-icon>
          <input
            v-model="workKeyword"
            type="search"
            placeholder="搜索作品、专辑或风格"
            aria-label="搜索作品、专辑或风格"
          />
        </label>

        <div class="filter-group" role="group" aria-label="按审核状态筛选">
          <button
            v-for="option in statusFilterOptions"
            :key="option.value"
            type="button"
            class="filter-chip"
            :class="{ active: workStatus === option.value }"
            :aria-pressed="workStatus === option.value"
            @click="setWorkStatus(option.value)"
          >
            {{ option.label }}
            <span>{{ option.count }}</span>
          </button>
        </div>

        <label class="sort-select">
          <span>排序</span>
          <select v-model="workSort" aria-label="作品排序方式">
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </div>

      <div v-if="selectedCount" class="bulk-bar" role="region" aria-label="作品批量操作">
        <div class="bulk-summary">
          <strong>已选 {{ selectedCount }} 首</strong>
          <span>对选中作品执行批量操作</span>
        </div>
        <div class="bulk-actions">
          <button type="button" class="button button-quiet button-small" :disabled="!batchOfflineAllowed" :title="batchOfflineAllowed ? '下架选中作品' : '仅已通过作品可以下架'" @click="batchOffline">
            批量下架
          </button>
          <button type="button" class="button button-danger button-small" @click="batchDelete">删除</button>
          <button type="button" class="text-button" @click="clearSelection">取消选择</button>
        </div>
      </div>

      <div v-if="workActionError" class="inline-alert" role="alert">
        <span>{{ workActionError }}</span>
        <button type="button" @click="workActionError = ''">关闭</button>
      </div>

      <div class="table-panel" :aria-busy="worksLoading">
        <div v-if="worksLoading" class="work-skeleton" aria-hidden="true">
          <div v-for="row in 5" :key="row" class="skeleton-row">
            <span class="skeleton skeleton-check"></span>
            <span class="skeleton skeleton-cover"></span>
            <span class="skeleton skeleton-copy"></span>
            <span class="skeleton skeleton-pill"></span>
            <span class="skeleton skeleton-number"></span>
            <span class="skeleton skeleton-number"></span>
          </div>
        </div>
        <p v-if="worksLoading" class="sr-only" role="status">正在加载作品</p>

        <div v-else-if="worksError" class="state-block is-error" role="alert">
          <el-icon><Warning /></el-icon>
          <strong>作品列表加载失败</strong>
          <p>{{ worksError }}</p>
          <button type="button" class="button button-quiet button-small" @click="loadWorks">
            <el-icon><Refresh /></el-icon>
            重试
          </button>
        </div>

        <div v-else-if="!works.length" class="state-block">
          <el-icon><Headset /></el-icon>
          <strong>还没有作品</strong>
          <p>上传第一首原创歌曲，提交平台审核后即可在曲库中发行。</p>
          <button type="button" class="button button-primary button-small" @click="openWorkEditor()">上传作品</button>
        </div>

        <div v-else-if="!filteredWorks.length" class="state-block">
          <el-icon><Search /></el-icon>
          <strong>没有符合条件的作品</strong>
          <p>当前筛选组合下没有结果。清空搜索或切换状态后再试。</p>
          <button type="button" class="button button-quiet button-small" @click="clearFilters">清空筛选</button>
        </div>

        <template v-else>
          <div class="table-scroll">
            <table class="works-table">
              <caption class="sr-only">原创作品列表，可按状态筛选、排序并执行批量操作</caption>
              <thead>
                <tr>
                  <th class="check-cell" scope="col">
                    <input
                      type="checkbox"
                      :checked="allPageSelected"
                      :indeterminate="somePageSelected && !allPageSelected"
                      aria-label="选择当前页全部作品"
                      @change="togglePageSelection"
                    />
                  </th>
                  <th scope="col">作品</th>
                  <th scope="col">状态</th>
                  <th class="number-cell" scope="col">播放</th>
                  <th class="number-cell" scope="col">收藏</th>
                  <th class="number-cell" scope="col">喜欢</th>
                  <th class="number-cell" scope="col">评论</th>
                  <th class="date-cell" scope="col">更新</th>
                  <th class="action-cell" scope="col">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in pagedWorks" :key="row.id" :class="{ selected: isSelected(row.id) }">
                  <td class="check-cell">
                    <input
                      type="checkbox"
                      :checked="isSelected(row.id)"
                      :aria-label="`选择作品 ${row.name}`"
                      @change="toggleSelection(row.id)"
                    />
                  </td>
                  <td>
                    <div class="song-cell">
                      <img :src="row.coverUrl || fallbackCover" :alt="`${row.name}封面`" @error="onCoverError" />
                      <div class="song-copy">
                        <strong>{{ row.name }}</strong>
                        <span>{{ row.albumName || '单曲' }} · {{ row.language || '未设置' }} · {{ row.style || '未分类' }}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span :class="['state-pill', workStatusClass(row.status)]">{{ workStatusText(row.status) }}</span>
                    <span v-if="row.rejectReason" class="reason-note" :title="row.rejectReason">查看原因</span>
                  </td>
                  <td class="number-cell">{{ formatNum(row.playCount) }}</td>
                  <td class="number-cell">{{ formatNum(row.collectCount) }}</td>
                  <td class="number-cell">{{ formatNum(row.likeCount) }}</td>
                  <td class="number-cell">{{ formatNum(row.commentCount) }}</td>
                  <td class="date-cell">{{ formatDate(row.updateTime || row.createTime) }}</td>
                  <td class="action-cell">
                    <div class="row-actions">
                      <button type="button" class="row-action" @click="openWorkEditor(row)">编辑</button>
                      <button v-if="row.status === 'APPROVED'" type="button" class="row-action" @click="offlineWork(row)">下架</button>
                      <button
                        v-else-if="row.status === 'OFFLINE'"
                        type="button"
                        class="row-action is-disabled"
                        disabled
                        title="当前后端未开放创作者自行上架，需平台审核"
                      >上架</button>
                      <button
                        v-else-if="row.status === 'REJECTED'"
                        type="button"
                        class="row-action is-disabled"
                        disabled
                        title="重新送审接口暂未开放，可先编辑并联系平台运营"
                      >重提</button>
                      <button type="button" class="row-action is-danger" @click="removeWork(row)">删除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="work-cards">
            <article v-for="row in pagedWorks" :key="row.id" class="work-card" :class="{ selected: isSelected(row.id) }">
              <div class="work-card-head">
                <input
                  type="checkbox"
                  :checked="isSelected(row.id)"
                  :aria-label="`选择作品 ${row.name}`"
                  @change="toggleSelection(row.id)"
                />
                <img :src="row.coverUrl || fallbackCover" :alt="`${row.name}封面`" @error="onCoverError" />
                <div class="work-card-copy">
                  <strong>{{ row.name }}</strong>
                  <span>{{ row.albumName || '单曲' }} · {{ row.style || '未分类' }}</span>
                </div>
                <span :class="['state-pill', workStatusClass(row.status)]">{{ workStatusText(row.status) }}</span>
              </div>
              <dl class="work-card-metrics">
                <div><dt>播放</dt><dd>{{ formatNum(row.playCount) }}</dd></div>
                <div><dt>收藏</dt><dd>{{ formatNum(row.collectCount) }}</dd></div>
                <div><dt>喜欢</dt><dd>{{ formatNum(row.likeCount) }}</dd></div>
                <div><dt>评论</dt><dd>{{ formatNum(row.commentCount) }}</dd></div>
              </dl>
              <div class="work-card-foot">
                <span>{{ formatDate(row.updateTime || row.createTime) }}</span>
                <div class="row-actions">
                  <button type="button" class="row-action" @click="openWorkEditor(row)">编辑</button>
                  <button v-if="row.status === 'APPROVED'" type="button" class="row-action" @click="offlineWork(row)">下架</button>
                  <button v-else-if="row.status === 'OFFLINE'" type="button" class="row-action is-disabled" disabled title="上架需平台审核">上架</button>
                  <button v-else-if="row.status === 'REJECTED'" type="button" class="row-action is-disabled" disabled title="重新送审接口暂未开放">重提</button>
                  <button type="button" class="row-action is-danger" @click="removeWork(row)">删除</button>
                </div>
              </div>
            </article>
          </div>

          <div class="table-footer">
            <span>第 {{ pageStart }}-{{ pageEnd }} 条，共 {{ filteredWorks.length }} 条</span>
            <div class="pager" aria-label="作品分页">
              <button type="button" :disabled="workPage <= 1" aria-label="上一页" @click="workPage--">上一页</button>
              <span>{{ workPage }} / {{ pageCount }}</span>
              <button type="button" :disabled="workPage >= pageCount" aria-label="下一页" @click="workPage++">下一页</button>
            </div>
          </div>
        </template>
      </div>

      <section v-if="works.length && !worksError" class="release-checks" aria-labelledby="release-check-title">
        <div class="release-head">
          <div>
            <h2 id="release-check-title">发行前检查</h2>
            <p>从资料完整度到审核队列，处理下一件会影响发行的事。</p>
          </div>
          <span class="section-note">{{ completeCount }}/{{ works.length }} 首资料齐全</span>
        </div>
        <div class="release-grid">
          <article class="release-card">
            <div class="release-card-head"><span>资料完整度</span><strong>{{ completeCount }}/{{ works.length }}</strong></div>
            <dl class="check-list">
              <div><dt>音频</dt><dd>{{ audioReadyCount }}/{{ works.length }}</dd></div>
              <div><dt>封面</dt><dd>{{ coverReadyCount }}/{{ works.length }}</dd></div>
              <div><dt>歌词</dt><dd>{{ lyricReadyCount }}/{{ works.length }}</dd></div>
            </dl>
            <button v-if="missingLyricWorks.length" type="button" class="text-button release-action" @click="openWorkEditor(missingLyricWorks[0])">补齐歌词</button>
            <span v-else class="release-done">资料已齐全</span>
          </article>
          <article class="release-card">
            <div class="release-card-head"><span>审核队列</span><strong>{{ pendingCount }} 首</strong></div>
            <ul v-if="pendingWorks.length" class="pending-list">
              <li v-for="work in pendingWorks" :key="work.id">
                <span>{{ work.name }}</span>
                <time :datetime="work.createTime">{{ formatDate(work.createTime) }}</time>
              </li>
            </ul>
            <p v-else class="release-empty">暂无待审核作品。</p>
            <button v-if="pendingCount" type="button" class="text-button release-action" @click="setWorkStatus('PENDING')">查看审核队列</button>
          </article>
          <article class="release-card release-rules">
            <div class="release-card-head"><span>发行规则</span></div>
            <ul>
              <li>审核通过后自动同步曲库上架。</li>
              <li>修改已通过作品会重新进入审核。</li>
              <li>自主上架与重新送审接口暂未开放。</li>
            </ul>
          </article>
        </div>
      </section>
    </div>

    <div
      v-else-if="tab === 'albums'"
      id="musician-panel-albums"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-albums"
      tabindex="0"
    >
      <div class="section-head">
        <div>
          <h2>专辑管理</h2>
          <p>维护专辑信息与曲目归属。后端专辑发布后即时生效，没有单独审核状态。</p>
        </div>
        <button type="button" class="button button-primary button-small" @click="openAlbumEditor()">
          <el-icon><Plus /></el-icon>
          新建专辑
        </button>
      </div>

      <div v-if="albumActionError" class="inline-alert" role="alert">
        <span>{{ albumActionError }}</span>
        <button type="button" @click="albumActionError = ''">关闭</button>
      </div>

      <div v-if="albumsLoading" class="album-grid" aria-hidden="true">
        <div v-for="n in 3" :key="n" class="album-card is-skeleton">
          <span class="skeleton skeleton-album-cover"></span>
          <span class="skeleton skeleton-line"></span>
          <span class="skeleton skeleton-line short"></span>
        </div>
      </div>
      <p v-if="albumsLoading" class="sr-only" role="status">正在加载专辑</p>

      <div v-else-if="albumsError" class="state-block is-error" role="alert">
        <el-icon><Warning /></el-icon>
        <strong>专辑列表加载失败</strong>
        <p>{{ albumsError }}</p>
        <button type="button" class="button button-quiet button-small" @click="loadAlbums">
          <el-icon><Refresh /></el-icon>
          重试
        </button>
      </div>

      <div v-else-if="!albumCards.length" class="state-block">
        <el-icon><Collection /></el-icon>
        <strong>还没有专辑</strong>
        <p>把单曲收进专辑，给听众一条完整的收听路径。</p>
        <button type="button" class="button button-primary button-small" @click="openAlbumEditor()">新建专辑</button>
      </div>

      <div v-else class="album-grid">
        <article v-for="album in albumCards" :key="album.__draft ? album.localId : album.id" class="album-card">
          <div class="album-cover">
            <img :src="album.coverUrl || fallbackCover" :alt="`${album.name}封面`" @error="onCoverError" />
          </div>
          <div class="album-card-body">
            <div class="album-heading">
              <strong>{{ album.name }}</strong>
              <span :class="['state-pill', album.__draft ? 'is-draft' : 'is-positive']">{{ albumStatusText(album) }}</span>
            </div>
            <p>{{ album.intro || '暂无专辑简介' }}</p>
            <div class="album-meta">
              <span>{{ album.releaseDate || '未设置发行日期' }}</span>
              <span>{{ albumSongCount(album) }} 首作品</span>
            </div>
          </div>
          <div class="album-actions">
            <button type="button" class="row-action" @click="openAlbumEditor(album)">编辑</button>
            <button
              v-if="album.__draft !== true"
              type="button"
              class="row-action"
              @click="toggleAlbumStatus(album)"
            >{{ album.status === 1 ? '下架' : '上架' }}</button>
            <button type="button" class="row-action is-danger" @click="removeAlbum(album)">删除</button>
          </div>
        </article>
      </div>
    </div>

    <div
      v-else-if="tab === 'stats'"
      id="musician-panel-stats"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-stats"
      tabindex="0"
    >
      <div class="section-head stats-head">
        <div>
          <h2>数据统计</h2>
          <p>播放与互动为累计值，趋势图按所选周期读取后端统计记录。</p>
        </div>
        <div class="period-switch" role="group" aria-label="统计周期">
          <button
            v-for="item in periods"
            :key="item.value"
            type="button"
            :class="{ active: statsPeriod === item.value }"
            :aria-pressed="statsPeriod === item.value"
            @click="changeStatsPeriod(item.value)"
          >{{ item.label }}</button>
        </div>
      </div>

      <div v-if="summaryError" class="inline-alert" role="alert">
        <span>{{ summaryError }}</span>
        <button type="button" @click="loadStats">重试</button>
      </div>

      <div class="metric-strip" :aria-busy="statsLoading">
        <div v-for="card in statCards" :key="card.label" class="metric-cell">
          <span class="metric-label">{{ card.label }}</span>
          <span v-if="statsLoading" class="skeleton metric-skeleton" aria-hidden="true"></span>
          <strong v-else class="metric-value" :class="{ 'is-muted': card.muted }">{{ card.value }}</strong>
          <span class="metric-note">{{ card.note }}</span>
        </div>
      </div>
      <p v-if="statsLoading" class="sr-only" role="status">正在加载数据统计</p>

      <div class="stats-grid">
        <section class="panel trend-panel" aria-labelledby="trend-title">
          <div class="panel-head">
            <div>
              <h3 id="trend-title">播放趋势</h3>
              <p>{{ statsPeriodLabel }} · 数据点 {{ trend.length }}</p>
            </div>
            <span v-if="!trendLoading && trend.length" class="panel-figure">
              {{ formatNum(trendTotal) }} <small>次播放</small>
            </span>
          </div>

          <div v-if="trendLoading" class="chart-skeleton" aria-hidden="true">
            <span v-for="n in 8" :key="n" class="skeleton" :style="{ height: `${28 + ((n * 17) % 78)}px` }"></span>
          </div>
          <p v-if="trendLoading" class="sr-only" role="status">正在加载播放趋势</p>

          <div v-else-if="trendLimited" class="state-block is-compact" role="status">
            <el-icon><TrendCharts /></el-icon>
            <strong>90 天趋势暂不可用</strong>
            <p>现有趋势接口最多返回近 30 天，未使用补齐或推测数据。</p>
            <button type="button" class="button button-quiet button-small" @click="viewThirtyDays">查看近 30 天</button>
          </div>

          <div v-else-if="trendError" class="state-block is-compact is-error" role="alert">
            <el-icon><Warning /></el-icon>
            <strong>播放趋势加载失败</strong>
            <p>{{ trendError }}</p>
            <button type="button" class="button button-quiet button-small" @click="loadStats">
              <el-icon><Refresh /></el-icon>
              重试
            </button>
          </div>

          <div v-else-if="!trend.length" class="state-block is-compact">
            <el-icon><TrendCharts /></el-icon>
            <strong>所选周期暂无播放记录</strong>
            <p>接口返回 0 条趋势数据，列表保持为空，不补造数据。</p>
          </div>

          <EChart v-else :option="trendOption" height="286px" />
        </section>

        <section class="panel rank-panel" aria-labelledby="rank-title">
          <div class="panel-head">
            <div>
              <h3 id="rank-title">作品表现排行</h3>
              <p>按累计播放量</p>
            </div>
          </div>

          <div v-if="rankLoading" class="rank-skeleton" aria-hidden="true">
            <div v-for="n in 4" :key="n" class="rank-skeleton-row">
              <span class="skeleton skeleton-cover"></span>
              <span class="skeleton skeleton-copy"></span>
              <span class="skeleton skeleton-number"></span>
            </div>
          </div>
          <p v-if="rankLoading" class="sr-only" role="status">正在加载作品排行</p>

          <div v-else-if="rankError" class="state-block is-compact is-error" role="alert">
            <el-icon><Warning /></el-icon>
            <strong>排行加载失败</strong>
            <p>{{ rankError }}</p>
            <button type="button" class="button button-quiet button-small" @click="loadStats">重试</button>
          </div>
          <div v-else-if="!rankWorks.length" class="state-block is-compact">
            <strong>暂无作品排行</strong>
            <p>作品产生播放后，这里会按数据排序。</p>
          </div>
          <ol v-else class="rank-list">
            <li v-for="(item, index) in rankWorks.slice(0, 5)" :key="item.id" class="rank-item">
              <span class="rank-index">{{ index + 1 }}</span>
              <img :src="item.coverUrl || fallbackCover" :alt="`${item.name}封面`" @error="onCoverError" />
              <div class="rank-copy">
                <div class="rank-heading"><strong>{{ item.name }}</strong><span>{{ formatNum(item.playCount) }}</span></div>
                <div class="rank-track" aria-hidden="true"><span :style="{ width: `${rankBarWidth(item)}%` }"></span></div>
                <div class="rank-meta">收藏 {{ formatNum(item.collectCount) }} · 喜欢 {{ formatNum(item.likeCount) }}</div>
              </div>
            </li>
          </ol>
        </section>
      </div>

      <div class="lower-grid">
        <section class="panel" aria-labelledby="region-title">
          <div class="panel-head">
            <div>
              <h3 id="region-title">听众地域</h3>
              <p>按粉丝记录中的地域字段聚合</p>
            </div>
            <span v-if="!regionLoading && regionRows.length" class="panel-figure">{{ formatNum(regionTotal) }} <small>条记录</small></span>
          </div>
          <div v-if="regionLoading" class="region-skeleton" aria-hidden="true">
            <span v-for="n in 4" :key="n" class="skeleton"></span>
          </div>
          <div v-else-if="regionError" class="state-block is-compact is-error" role="alert">
            <span>{{ regionError }}</span>
            <button type="button" class="button button-quiet button-small" @click="loadStats">重试</button>
          </div>
          <div v-else-if="!regionRows.length" class="state-block is-compact">
            <strong>暂无地域数据</strong>
            <p>粉丝资料中填写地域后才会汇总到这里。</p>
          </div>
          <div v-else class="region-list">
            <div v-for="row in regionRows" :key="row.name" class="region-row">
              <span>{{ row.name }}</span>
              <div class="region-track" aria-hidden="true"><span :style="{ width: `${regionWidth(row.value)}%` }"></span></div>
              <strong>{{ formatNum(row.value) }}</strong>
            </div>
          </div>
        </section>

        <aside class="panel data-notes" aria-labelledby="quality-title">
          <div class="panel-head">
            <div>
              <h3 id="quality-title">数据口径</h3>
              <p>当前接口能提供与不能提供的内容</p>
            </div>
          </div>
          <ul>
            <li>播放、喜欢、评论、粉丝数来自音乐人统计接口。</li>
            <li>收藏数由作品列表中的收藏字段汇总。</li>
            <li>粉丝增长暂无趋势接口，以占位符显示，未推算。</li>
            <li>90 天趋势超出后端 30 天上限，保留限制态。</li>
          </ul>
        </aside>
      </div>
    </div>

    <div
      v-else-if="tab === 'income'"
      id="musician-panel-income"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-income"
      tabindex="0"
    >
      <div class="section-head">
        <div>
          <h2>收益管理</h2>
          <p>播放和礼物收益按平台规则结算，提现申请进入人工审核。</p>
        </div>
        <div class="head-actions">
          <button type="button" class="button button-quiet button-small" :disabled="settling || incomeLoading" @click="settleIncome">
            <el-icon><Promotion /></el-icon>
            {{ settling ? '结算中…' : '结算收益' }}
          </button>
          <button type="button" class="button button-primary button-small" :disabled="!canWithdraw || incomeLoading" :title="canWithdraw ? '申请提现' : '当前没有可提现余额'" @click="openWithdraw">
            申请提现
          </button>
        </div>
      </div>

      <div v-if="incomeError" class="inline-alert" role="alert">
        <span>{{ incomeError }}</span>
        <button type="button" @click="loadIncome">重试</button>
      </div>

      <template v-if="incomeLoading">
        <div class="income-skeleton" aria-hidden="true">
          <span class="skeleton income-lead-skeleton"></span>
          <div class="metric-strip">
            <span v-for="n in 3" :key="n" class="skeleton metric-skeleton"></span>
          </div>
          <span class="skeleton income-table-skeleton"></span>
        </div>
        <p class="sr-only" role="status">正在加载收益数据</p>
      </template>

      <template v-else>
        <section class="income-lead" aria-labelledby="balance-title">
          <div class="balance-copy">
            <span id="balance-title">可提现金额</span>
            <strong>¥{{ formatMoney(income.balance) }}</strong>
            <p>账户余额 ¥{{ formatMoney(income.balance) }} · 后端当前未设置冻结金额</p>
          </div>
          <div class="settlement-state">
            <span class="state-pill" :class="latestSettlement?.status === 'SUCCESS' ? 'is-positive' : 'is-warning'">
              {{ latestSettlement?.status === 'SUCCESS' ? '最近结算已入账' : latestSettlement ? '结算处理中' : '暂无结算记录' }}
            </span>
            <small v-if="latestSettlement">{{ incomeTypeText(latestSettlement.type) }} · {{ formatDateTime(latestSettlement.createTime) }}</small>
            <small v-else>完成首次结算后会在这里显示状态</small>
          </div>
        </section>

        <div class="metric-strip income-metrics">
          <div class="metric-cell">
            <span class="metric-label">累计收益</span>
            <strong class="metric-value">¥{{ formatMoney(income.totalIncome) }}</strong>
            <span class="metric-note">含播放与礼物结算</span>
          </div>
          <div class="metric-cell">
            <span class="metric-label">已提现</span>
            <strong class="metric-value">¥{{ formatMoney(income.withdrawn) }}</strong>
            <span class="metric-note">已审核通过的提现</span>
          </div>
          <div class="metric-cell">
            <span class="metric-label">待处理提现</span>
            <strong class="metric-value">¥{{ formatMoney(pendingWithdrawAmount) }}</strong>
            <span class="metric-note">{{ pendingWithdrawCount }} 笔申请等待审核</span>
          </div>
        </div>

        <div class="income-grid">
          <section class="panel" aria-labelledby="income-detail-title">
            <div class="panel-head">
              <div>
                <h3 id="income-detail-title">收益明细</h3>
                <p>共 {{ incomeTotal }} 条记录</p>
              </div>
              <button type="button" class="icon-button" aria-label="刷新收益明细" @click="loadIncome">
                <el-icon><Refresh /></el-icon>
              </button>
            </div>
            <div v-if="!incomes.length" class="state-block is-compact">
              <el-icon><Wallet /></el-icon>
              <strong>暂无收益明细</strong>
              <p>完成结算后，播放和礼物收益会逐条记录。</p>
            </div>
            <div v-else class="record-list">
              <div v-for="row in incomes" :key="row.id" class="record-row">
                <div class="record-copy">
                  <strong>{{ incomeTypeText(row.type) }}</strong>
                  <span>{{ row.description || '—' }}</span>
                </div>
                <span :class="['state-pill', incomeStatusClass(row.status)]">{{ incomeStatusText(row.status) }}</span>
                <time :datetime="row.createTime">{{ formatDateTime(row.createTime) }}</time>
                <strong :class="['record-amount', { negative: Number(row.amount) < 0 }]">
                  {{ Number(row.amount) >= 0 ? '+' : '−' }}¥{{ formatMoney(Math.abs(Number(row.amount) || 0)) }}
                </strong>
              </div>
            </div>
          </section>

          <aside class="income-side">
            <section class="panel" aria-labelledby="withdraw-title">
              <div class="panel-head">
                <div>
                  <h3 id="withdraw-title">提现记录</h3>
                  <p>{{ withdrawTotal }} 条申请</p>
                </div>
              </div>
              <div v-if="!withdraws.length" class="state-block is-compact">
                <el-icon><CreditCard /></el-icon>
                <strong>还没有提现申请</strong>
                <p>可提现金额大于 0 时，可在页面右上角发起提现。</p>
              </div>
              <div v-else class="withdraw-list">
                <div v-for="row in withdraws" :key="row.id" class="withdraw-item">
                  <div class="withdraw-top">
                    <strong>¥{{ formatMoney(row.amount) }}</strong>
                    <span :class="['state-pill', withdrawStatusClass(row.status)]">{{ withdrawStatusText(row.status) }}</span>
                  </div>
                  <span>{{ withdrawMethodLabel(row.payType) }} · {{ maskAccount(row.account) }}</span>
                  <time :datetime="row.createTime">{{ formatDateTime(row.createTime) }}</time>
                  <p v-if="row.remark">{{ row.remark }}</p>
                </div>
              </div>
            </section>

            <div class="risk-note" role="note">
              <el-icon><Warning /></el-icon>
              <div>
                <strong>提现风险提示</strong>
                <p>收益结算和提现均以平台审核结果为准。提现仅支持支付宝或微信，提交申请不代表立即到账；请确认收款账号实名与本人一致，因账户信息错误导致的退回由申请人承担。</p>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>

    <div
      v-else-if="tab === 'singer'"
      id="musician-panel-singer"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-singer"
      tabindex="0"
    >
      <div class="section-head">
        <div>
          <h2>歌手认证</h2>
          <p>只有已认证音乐人才能申请成为平台认证歌手。</p>
        </div>
        <span :class="['state-pill', singerStateClass]">{{ singerStateText }}</span>
      </div>

      <div v-if="singerError" class="inline-alert" role="alert">
        <span>{{ singerError }}</span>
        <button type="button" @click="loadSinger(true)">重试</button>
      </div>

      <div v-if="singerLoading" class="singer-cert-layout" aria-hidden="true">
        <span class="skeleton singer-skeleton"></span>
        <span class="skeleton singer-skeleton"></span>
      </div>

      <div v-else class="singer-cert-layout">
        <section class="panel singer-identity-card" aria-labelledby="singer-identity-title">
          <div class="panel-head">
            <div>
              <h3 id="singer-identity-title">音乐人身份</h3>
              <p>歌手认证会同步以下资料</p>
            </div>
          </div>
          <div class="singer-profile-main">
            <el-avatar :size="64" :src="musician?.avatar" class="singer-profile-avatar">
              {{ (musician?.stageName || '音').slice(0, 1) }}
            </el-avatar>
            <div>
              <strong class="singer-profile-name">{{ musician?.stageName || '音乐人' }}</strong>
              <span class="singer-profile-meta">{{ musician?.style || '独立音乐人' }} · {{ musician?.status === 'APPROVED' ? '已认证音乐人' : '音乐人认证审核中' }}</span>
            </div>
          </div>
          <p class="singer-profile-intro">{{ musician?.intro || '还没有填写音乐人简介。' }}</p>
          <div class="singer-rule">
            <el-icon><Microphone /></el-icon>
            <span>只有已通过音乐人认证的账号才能申请歌手认证；审核通过后，歌手主页会展示在「歌手」频道。</span>
          </div>
        </section>

        <section class="panel singer-action-card" aria-labelledby="singer-action-title">
          <template v-if="singerInfo?.status === 1">
            <div class="singer-result-mark is-approved" aria-hidden="true">✓</div>
            <h3 id="singer-action-title">已认证歌手</h3>
            <p>你的歌手身份已通过平台审核，可以在歌手频道展示主页、歌曲与粉丝数据。</p>
            <router-link :to="`/singer/${singerInfo.id}`" class="button button-primary button-small singer-result-link">查看歌手主页</router-link>
          </template>

          <template v-else-if="singerInfo?.status === 0">
            <div class="singer-result-mark is-pending" aria-hidden="true">◷</div>
            <h3 id="singer-action-title">歌手认证审核中</h3>
            <p>平台正在核验你的音乐人资料与作品，通常会在 1–3 个工作日内完成审核。</p>
            <span class="singer-submitted-at">提交于 {{ formatDateTime(singerInfo.createTime) }}</span>
          </template>

          <template v-else>
            <div class="panel-head">
              <div>
                <h3 id="singer-action-title">{{ singerInfo?.status === 2 ? '重新提交认证' : '提交歌手认证' }}</h3>
                <p>认证通过后将获得歌手主页与歌手频道曝光</p>
              </div>
            </div>
            <div v-if="singerInfo?.status === 2 && singerInfo.rejectReason" class="singer-reject-note" role="alert">
              <strong>上次审核未通过</strong>
              <span>{{ singerInfo.rejectReason }}</span>
            </div>
            <form class="editor-form singer-apply-form" @submit.prevent="submitSingerApply">
              <div class="field-grid">
                <label class="field" for="singer-region">
                  <span>地区 <em>*</em></span>
                  <select id="singer-region" v-model="singerForm.region">
                    <option value="国内">国内</option>
                    <option value="国外">国外</option>
                    <option value="粤语">粤语</option>
                  </select>
                </label>
                <label class="field" for="singer-gender">
                  <span>性别</span>
                  <select id="singer-gender" v-model="singerForm.gender">
                    <option value="男">男</option>
                    <option value="女">女</option>
                  </select>
                </label>
                <label class="field field-wide" for="singer-intro">
                  <span>歌手简介</span>
                  <textarea id="singer-intro" v-model="singerForm.intro" maxlength="300" rows="3" placeholder="介绍一下你的音乐风格与代表作品"></textarea>
                </label>
              </div>
              <p class="singer-form-note">提交后将使用音乐人艺名「{{ musician?.stageName }}」与头像创建歌手档案，进入人工审核。</p>
              <button type="submit" class="button button-primary singer-submit" :disabled="singerSubmitting || musician?.status !== 'APPROVED'">
                {{ singerSubmitting ? '提交中…' : singerInfo?.status === 2 ? '重新提交审核' : '提交认证' }}
              </button>
              <p v-if="musician?.status !== 'APPROVED'" class="singer-form-warning">音乐人认证通过后才能提交歌手认证。</p>
            </form>
          </template>
        </section>
      </div>
    </div>

    <div
      v-else-if="tab === 'mvs'"
      id="musician-panel-mvs"
      class="console-panel"
      role="tabpanel"
      aria-labelledby="musician-tab-mvs"
      tabindex="0"
    >
      <div class="section-head">
        <div>
          <h2>MV 管理</h2>
          <p>歌手与音乐人都可以发布音乐 MV，提交后进入平台审核。</p>
        </div>
        <button type="button" class="button button-primary button-small" :disabled="!canPublishMv" @click="openMvDialog">
          <el-icon><VideoCamera /></el-icon>
          发布 MV
        </button>
      </div>

      <div v-if="!canPublishMv" class="inline-alert" role="note">
        <span>完成音乐人认证后才能发布 MV。认证通过后刷新本页即可使用。</span>
      </div>
      <div v-if="mvError" class="inline-alert" role="alert">
        <span>{{ mvError }}</span>
        <button type="button" @click="loadMvs">重试</button>
      </div>

      <div v-if="mvLoading" class="mv-creator-grid" aria-hidden="true">
        <span v-for="n in 3" :key="n" class="skeleton mv-card-skeleton"></span>
      </div>
      <div v-else-if="!mvs.length" class="state-block is-compact">
        <el-icon><VideoCamera /></el-icon>
        <strong>还没有发布 MV</strong>
        <p>上传一支音乐 MV，审核通过后会出现在 MV 专区。</p>
      </div>
      <div v-else class="mv-creator-grid">
        <article v-for="mv in mvs" :key="mv.id" class="mv-own-card">
          <div class="mv-own-cover">
            <img v-if="mv.coverUrl" :src="mv.coverUrl" :alt="mv.name" />
            <div v-else class="mv-own-placeholder" aria-hidden="true">▶</div>
            <span class="mv-own-quality">{{ mv.quality || 'HD' }}</span>
          </div>
          <div class="mv-own-copy">
            <div class="mv-own-head">
              <strong>{{ mv.name }}</strong>
              <span :class="['state-pill', mvAuditClass(mv)]">{{ mvAuditText(mv) }}</span>
            </div>
            <span class="mv-own-meta">{{ mv.singerName || '未关联歌手' }} · {{ formatDuration(mv.duration) }}</span>
            <div class="mv-own-foot">
              <span>{{ formatDateTime(mv.createTime) }}</span>
              <span v-if="mv.status === 1">· {{ formatNum(mv.playCount || 0) }} 次播放</span>
            </div>
            <p v-if="mv.auditStatus === 'REJECTED' && mv.rejectReason" class="mv-own-reject">{{ mv.rejectReason }}</p>
            <div class="mv-own-actions">
              <button type="button" class="row-action" @click="openMvEdit(mv)">编辑</button>
              <button
                v-if="mv.status === 1"
                type="button"
                class="row-action"
                @click="setMvStatus(mv, 0)"
              >下架</button>
              <button v-else type="button" class="row-action" @click="setMvStatus(mv, 1)">上架</button>
              <button type="button" class="row-action is-danger" @click="removeMv(mv)">删除</button>
            </div>
          </div>
        </article>
      </div>
    </div>

    <el-dialog v-model="mvDialog" class="mv-publish-dialog" :title="mvEditId ? '编辑 MV' : '发布 MV'" width="min(640px, calc(100vw - 24px))" :close-on-click-modal="false" @closed="mvFormError = ''">
      <form class="editor-form mv-publish-form" @submit.prevent="submitMv">
        <div class="field-grid">
          <label class="field field-wide" for="mv-name">
            <span>MV 名称 <em>*</em></span>
            <input id="mv-name" v-model="mvForm.name" type="text" maxlength="80" placeholder="例如：城市夜归人 MV" />
          </label>
          <label class="field" for="mv-song">
            <span>关联歌曲</span>
            <select id="mv-song" v-model="mvForm.songId">
              <option :value="null">不关联歌曲</option>
              <option v-for="song in mvSongOptions" :key="song.id" :value="song.id">{{ song.name }}</option>
            </select>
          </label>
          <label class="field" for="mv-quality">
            <span>清晰度</span>
            <select id="mv-quality" v-model="mvForm.quality">
              <option value="HD">HD</option>
              <option value="UHD">UHD</option>
              <option value="SD">SD</option>
            </select>
          </label>
          <label class="field" for="mv-duration">
            <span>时长（秒）</span>
            <input id="mv-duration" v-model.number="mvForm.duration" type="number" min="0" step="1" placeholder="例如：240" />
          </label>
          <label class="field mv-vip-field">
            <span>会员权益</span>
            <label class="mv-check"><input v-model="mvForm.vipOnly" type="checkbox" /> VIP 专属高清 MV</label>
          </label>
        </div>
        <section class="form-section mv-upload-section">
          <div class="form-section-head"><h3>MV 视频 <em>*</em></h3><span>MP4 / MOV / FLV / MKV · 最大 500MB</span></div>
          <MediaDropzone v-model="mvForm.videoUrl" kind="video" biz-type="video" title="拖拽 MV 视频到这里，或选择文件" hint="上传完成后可填写时长，提交后进入平台审核" :max-size-mb="500" />
        </section>
        <section class="form-section mv-upload-section">
          <div class="form-section-head"><h3>视频封面</h3><span>JPG / PNG / WebP · 最大 10MB</span></div>
          <MediaDropzone v-model="mvForm.coverUrl" kind="cover" biz-type="cover" title="拖拽封面到这里，或选择图片" hint="建议使用 16:9 横版封面" :max-size-mb="10" />
        </section>
        <label class="field" for="mv-description">
          <span>MV 描述</span>
          <textarea id="mv-description" v-model="mvForm.description" maxlength="1000" rows="3" placeholder="介绍一下这支 MV 的拍摄想法或故事"></textarea>
        </label>
        <p v-if="mvFormError" class="inline-alert" role="alert">{{ mvFormError }}</p>
      </form>
      <template #footer>
        <div class="dialog-footer-actions">
          <button type="button" class="text-button" :disabled="mvSubmitting" @click="mvDialog = false">取消</button>
          <button type="button" class="button button-primary" :disabled="mvSubmitting" @click="submitMv">{{ mvSubmitting ? '提交中…' : '提交审核' }}</button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      v-model="workDrawer"
      class="work-editor-drawer"
      direction="rtl"
      size="680px"
      :with-header="false"
      :close-on-click-modal="false"
      aria-label="作品编辑器"
      @closed="resetWorkEditor"
    >
      <div class="drawer-shell">
        <header class="drawer-head">
          <div>
            <h2>{{ workForm.name || (editingWorkId ? '编辑作品' : '上传原创作品') }}</h2>
            <p>{{ editingWorkId ? '编辑作品' : '上传原创作品' }} · 保存后将进入平台审核，审核通过才会在曲库上架。</p>
          </div>
          <button type="button" class="drawer-close" aria-label="关闭作品编辑器" @click="workDrawer = false">关闭</button>
        </header>

        <div class="drawer-body">
          <form id="work-editor-form" class="editor-form" @submit.prevent="submitWork">
            <section class="form-section">
              <div class="form-section-head">
                <h3>基础信息</h3>
                <span>用于曲库展示</span>
              </div>
              <div class="field-grid">
                <label class="field field-wide" for="work-name">
                  <span>作品名称 <em>*</em></span>
                  <input
                    id="work-name"
                    v-model="workForm.name"
                    type="text"
                    maxlength="80"
                    :aria-invalid="submitted && !workForm.name.trim()"
                    placeholder="例如：城市夜归人"
                  />
                </label>
                <label class="field" for="work-language">
                  <span>语种</span>
                  <select id="work-language" v-model="workForm.language">
                    <option v-for="item in languageOptions" :key="item" :value="item">{{ item }}</option>
                  </select>
                </label>
                <label class="field" for="work-style">
                  <span>风格</span>
                  <select id="work-style" v-model="workForm.style">
                    <option v-for="item in styleOptions" :key="item" :value="item">{{ item }}</option>
                  </select>
                </label>
                <label class="field" for="work-album">
                  <span>所属专辑</span>
                  <select id="work-album" v-model="workForm.albumId" @change="onWorkAlbumChange">
                    <option value="">不加入专辑</option>
                    <option v-for="album in albums" :key="album.id" :value="album.id">{{ album.name }}</option>
                  </select>
                </label>
                <label class="field" for="work-release">
                  <span>发行日期</span>
                  <input id="work-release" v-model="workForm.releaseDate" type="date" />
                </label>
              </div>
            </section>

            <section class="form-section">
              <div class="form-section-head">
                <h3>音频母带</h3>
                <span>MP3 / WAV / FLAC / M4A / AAC · 最大 100MB</span>
              </div>
              <MediaDropzone
                v-model="workForm.audioUrl"
                kind="audio"
                biz-type="audio"
                title="拖拽音频到这里，或选择文件"
                hint="建议上传 320kbps 以上音质；上传完成后自动读取时长"
                :max-size-mb="100"
                @uploaded="onAudioUploaded"
              />
              <div class="file-meta-row">
                <span>时长：{{ formatDuration(workForm.duration) }}</span>
                <span v-if="workForm.audioUrl" class="verified-text">音频地址已就绪</span>
              </div>
            </section>

            <section class="form-section">
              <div class="form-section-head">
                <h3>封面</h3>
                <span>JPG / PNG / WebP / GIF / BMP · 最大 10MB</span>
              </div>
              <div class="cover-editor">
                <MediaDropzone
                  v-model="workForm.coverUrl"
                  kind="cover"
                  biz-type="cover"
                  title="拖拽封面到这里，或选择图片"
                  hint="建议使用 1:1 正方形封面，至少 800×800px"
                  :max-size-mb="10"
                />
                <div class="cover-large-preview">
                  <img v-if="workForm.coverUrl" :src="workForm.coverUrl" alt="封面大图预览" @error="onCoverError" />
                  <div v-else class="cover-placeholder" aria-hidden="true"><el-icon><Picture /></el-icon></div>
                  <span>1:1 预览</span>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="form-section-head">
                <h3>歌词</h3>
                <span>LRC / TXT · 最大 1MB</span>
              </div>
              <MediaDropzone
                v-model="workForm.lyricUrl"
                kind="lyric"
                biz-type="lyric"
                title="拖拽歌词文件到这里，或选择文件"
                hint="可选流程；粘贴 LRC 文本后提交时会自动生成歌词文件"
                :max-size-mb="1"
                @uploaded="onLyricUploaded"
              />
              <label class="field lyric-field" for="work-lyric">
                <span>歌词文本</span>
                <textarea
                  id="work-lyric"
                  v-model="lyricText"
                  rows="7"
                  placeholder="[00:00.00] 在这里粘贴 LRC 歌词&#10;[00:12.00] 每行以时间标签开头"
                  @input="lyricDirty = true"
                ></textarea>
                <small>歌词会随作品一起提交审核；修改文本后保存会自动上传新的 LRC 文件。</small>
              </label>
            </section>

            <section class="form-section">
              <div class="form-section-head">
                <h3>发行设置</h3>
                <span>审核通过后生效</span>
              </div>
              <label class="switch-row" for="work-vip">
                <span>
                  <strong>付费试听</strong>
                  <small>勾选后标记为 VIP 专属作品</small>
                </span>
                <input id="work-vip" v-model="workForm.vipOnly" type="checkbox" role="switch" />
              </label>
            </section>

            <section class="form-section copyright-section">
              <label class="copyright-check" for="work-copyright">
                <input id="work-copyright" v-model="copyrightAccepted" type="checkbox" :aria-invalid="submitted && !copyrightAccepted" />
                <span>
                  <strong>我确认拥有该作品的合法版权或授权 <em>*</em></strong>
                  <small>包括词曲、录音、封面及歌词文件；如产生侵权争议，由上传者承担相应责任。</small>
                </span>
              </label>
            </section>

            <div v-if="workFormError" class="inline-alert" role="alert">
              <span>{{ workFormError }}</span>
            </div>
          </form>
        </div>

        <footer class="drawer-foot">
          <button type="button" class="button button-quiet" :disabled="workSaving" @click="saveWorkDraft">保存草稿</button>
          <div class="drawer-foot-actions">
            <button type="button" class="text-button" :disabled="workSaving" @click="workDrawer = false">取消</button>
            <button type="button" class="button button-primary" :disabled="workSaving" @click="submitWork">
              {{ workSaving ? '提交中…' : '保存并提交审核' }}
            </button>
          </div>
        </footer>
      </div>
    </el-drawer>

    <el-dialog
      v-model="albumDialog"
      class="album-editor-dialog"
      :title="albumEditSource === 'remote' ? '编辑专辑' : albumEditSource === 'draft' ? '编辑专辑草稿' : '新建专辑'"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <form class="editor-form album-form" @submit.prevent="publishAlbum">
        <div class="field-grid">
          <label class="field field-wide" for="album-name">
            <span>专辑名称 <em>*</em></span>
            <input id="album-name" v-model="albumForm.name" type="text" maxlength="80" placeholder="例如：城市夜归人" />
          </label>
          <label class="field field-wide" for="album-intro">
            <span>专辑简介</span>
            <textarea id="album-intro" v-model="albumForm.intro" rows="3" maxlength="500" placeholder="介绍这张专辑的创作背景与概念"></textarea>
          </label>
          <label class="field" for="album-release">
            <span>发行日期</span>
            <input id="album-release" v-model="albumForm.releaseDate" type="date" />
          </label>
          <div class="field">
            <span>发布状态</span>
            <div class="status-readout">
              <span :class="['state-pill', albumEditSource === 'remote' ? 'is-positive' : 'is-draft']">
                {{ albumEditSource === 'remote' ? '已发布' : '草稿' }}
              </span>
              <small>{{ albumEditSource === 'remote' ? '保存后即时生效' : '发布后即时生效' }}</small>
            </div>
          </div>
        </div>

        <section class="album-cover-section">
          <div class="form-section-head">
            <h3>专辑封面</h3>
            <span>JPG / PNG / WebP · 最大 10MB</span>
          </div>
          <MediaDropzone
            v-model="albumForm.coverUrl"
            kind="cover"
            biz-type="cover"
            title="拖拽专辑封面到这里，或选择图片"
            hint="建议 1:1 正方形，至少 800×800px"
            :max-size-mb="10"
          />
        </section>

        <section class="album-tracks">
          <div class="form-section-head">
            <h3>收录作品</h3>
            <span>已选 {{ selectedWorkIds.length }} 首</span>
          </div>
          <label class="track-search">
            <el-icon aria-hidden="true"><Search /></el-icon>
            <input v-model="albumWorkKeyword" type="search" placeholder="搜索要加入专辑的作品" aria-label="搜索要加入专辑的作品" />
          </label>
          <div v-if="!works.length" class="track-empty">暂无可收录作品，请先上传作品。</div>
          <div v-else-if="!albumWorkOptions.length" class="track-empty">没有匹配的作品。</div>
          <div v-else class="track-options">
            <label v-for="work in albumWorkOptions" :key="work.id" class="track-option">
              <input type="checkbox" :checked="selectedWorkIds.includes(String(work.id))" @change="toggleAlbumWork(work.id)" />
              <img :src="work.coverUrl || fallbackCover" :alt="`${work.name}封面`" @error="onCoverError" />
              <span class="track-option-copy">
                <strong>{{ work.name }}</strong>
                <small>{{ workStatusText(work.status) }} · {{ formatNum(work.playCount) }} 次播放</small>
              </span>
            </label>
          </div>
          <p class="form-hint">添加或移除已通过作品会触发重新审核；草稿阶段不会请求后端。</p>
        </section>

        <div v-if="albumFormError" class="inline-alert" role="alert">
          <span>{{ albumFormError }}</span>
        </div>
      </form>

      <template #footer>
        <div class="dialog-footer">
          <button
            v-if="albumEditSource !== 'remote'"
            type="button"
            class="button button-quiet"
            :disabled="albumSaving"
            @click="saveAlbumDraft"
          >保存草稿</button>
          <div class="dialog-footer-actions">
            <button type="button" class="text-button" :disabled="albumSaving" @click="albumDialog = false">取消</button>
            <button type="button" class="button button-primary" :disabled="albumSaving" @click="publishAlbum">
              {{ albumSaving ? '发布中…' : albumEditSource === 'remote' ? '保存并发布' : '发布专辑' }}
            </button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawDialog" class="withdraw-dialog" title="申请提现" width="440px" :close-on-click-modal="false">
      <div class="withdraw-balance">
        <span>当前可提现</span>
        <strong>¥{{ formatMoney(income.balance) }}</strong>
        <small>仅支持整数金额，最低 ¥1</small>
      </div>
      <form class="editor-form" @submit.prevent="submitWithdraw">
        <fieldset class="withdraw-method-field">
          <legend>提现方式 <em>*</em></legend>
          <div class="withdraw-method-grid" role="radiogroup" aria-label="选择提现方式">
            <button
              type="button"
              class="withdraw-method"
              :class="{ selected: withdrawForm.payType === 'ALIPAY' }"
              role="radio"
              :aria-checked="withdrawForm.payType === 'ALIPAY'"
              @click="selectWithdrawMethod('ALIPAY')"
            >
              <span class="withdraw-method-logo alipay" aria-hidden="true">支</span>
              <span class="withdraw-method-copy"><strong>支付宝</strong><small>到账支付宝余额</small></span>
              <span class="withdraw-radio" aria-hidden="true"></span>
            </button>
            <button
              type="button"
              class="withdraw-method"
              :class="{ selected: withdrawForm.payType === 'WECHAT' }"
              role="radio"
              :aria-checked="withdrawForm.payType === 'WECHAT'"
              @click="selectWithdrawMethod('WECHAT')"
            >
              <span class="withdraw-method-logo wechat" aria-hidden="true">微</span>
              <span class="withdraw-method-copy"><strong>微信</strong><small>到账微信零钱</small></span>
              <span class="withdraw-radio" aria-hidden="true"></span>
            </button>
          </div>
        </fieldset>
        <label class="field" for="withdraw-amount">
          <span>提现金额 <em>*</em></span>
          <input
            id="withdraw-amount"
            v-model.number="withdrawForm.amount"
            type="number"
            min="1"
            step="1"
            :max="Number(income.balance) || 0"
            placeholder="请输入整数金额"
          />
        </label>
        <label class="field" for="withdraw-account">
          <span>{{ withdrawForm.payType === 'WECHAT' ? '微信号' : '支付宝账号' }} <em>*</em></span>
          <input id="withdraw-account" v-model="withdrawForm.account" type="text" maxlength="80" :placeholder="withdrawAccountPlaceholder" />
          <small>需与实名认证信息一致，审核通过后按所选方式打款。</small>
        </label>
        <div v-if="withdrawError" class="inline-alert" role="alert">
          <span>{{ withdrawError }}</span>
        </div>
        <div class="risk-note is-inline" role="note">
          <el-icon><Warning /></el-icon>
          <div>
            <strong>提交前请核对账户</strong>
            <p>提现申请将进入人工审核，审核通过后才会计入已提现；平台会按所选支付宝或微信渠道打款，账号信息错误可能导致打款失败。</p>
          </div>
        </div>
      </form>
      <template #footer>
        <div class="dialog-footer-actions">
          <button type="button" class="text-button" :disabled="withdrawSubmitting" @click="withdrawDialog = false">取消</button>
          <button type="button" class="button button-primary" :disabled="withdrawSubmitting" @click="submitWithdraw">
            {{ withdrawSubmitting ? '提交中…' : '确认提交' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection,
  CreditCard,
  Headset,
  Picture,
  Plus,
  Promotion,
  Refresh,
  Search,
  TrendCharts,
  Upload,
  User,
  VideoCamera,
  Wallet,
  Warning
} from '@element-plus/icons-vue'
import { musicianApi, musicApi, fileApi } from '@/api'
import { useUserStore } from '@/stores/user'
import EChart from '@/components/EChart.vue'
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const router = useRouter()
const userStore = useUserStore()
const fallbackAvatar = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160"><rect width="160" height="160" fill="#19151f"/><circle cx="80" cy="62" r="24" fill="#3a3042"/><path d="M30 160c0-28 22-50 50-50s50 22 50 50z" fill="#3a3042"/></svg>'
)
const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="300" height="300" fill="#17131d"/><circle cx="150" cy="142" r="58" fill="#2a2233"/><circle cx="150" cy="142" r="13" fill="#6f5b7c"/><path d="M202 95v88a28 28 0 1 1-12-23V112l-50 12v70a28 28 0 1 1-12-23v-74z" fill="#6f5b7c"/></svg>'
)

const tabs = [
  { name: 'works', label: '作品管理' },
  { name: 'albums', label: '专辑管理' },
  { name: 'stats', label: '数据统计' },
  { name: 'income', label: '收益管理' },
  { name: 'singer', label: '歌手认证' },
  { name: 'mvs', label: 'MV 管理' }
]
const statusFilters = [
  { value: '', label: '全部', key: '' },
  { value: 'PENDING', label: '待审核', key: 'PENDING' },
  { value: 'APPROVED', label: '已通过', key: 'APPROVED' },
  { value: 'REJECTED', label: '未通过', key: 'REJECTED' },
  { value: 'OFFLINE', label: '已下架', key: 'OFFLINE' }
]
const sortOptions = [
  { value: 'updated_desc', label: '最近更新' },
  { value: 'created_desc', label: '最近创建' },
  { value: 'play_desc', label: '播放最多' },
  { value: 'collect_desc', label: '收藏最多' },
  { value: 'like_desc', label: '喜欢最多' }
]
const periods = [
  { value: '7', label: '近 7 天' },
  { value: '30', label: '近 30 天' },
  { value: '90', label: '近 90 天' }
]
const languageOptions = ['国语', '粤语', '英语', '日语', '韩语', '其他']
const styleOptions = ['流行', '摇滚', '民谣', '电子', '说唱', '古风', '爵士', 'R&B', '其他']

const tab = ref('works')
const musician = ref(null)
const albums = ref([])
const works = ref([])
const profileLoading = ref(false)
const profileError = ref('')
const albumsLoading = ref(false)
const albumsError = ref('')
const worksLoading = ref(false)
const worksError = ref('')
const workActionError = ref('')
const albumActionError = ref('')

const workKeyword = ref('')
const workStatus = ref('')
const workSort = ref('updated_desc')
const workPage = ref(1)
const pageSize = 20
const selectedIds = ref([])

const workDrawer = ref(false)
const editingWorkId = ref(null)
const workSaving = ref(false)
const submitted = ref(false)
const workFormError = ref('')
const copyrightAccepted = ref(false)
const lyricText = ref('')
const lyricDirty = ref(false)
const workDraft = ref(null)
const workForm = reactive(createEmptyWork())

const albumDialog = ref(false)
const albumEditSource = ref('new')
const albumEditId = ref(null)
const albumEditOriginal = ref(null)
const albumSaving = ref(false)
const albumFormError = ref('')
const albumWorkKeyword = ref('')
const selectedWorkIds = ref([])
const localAlbumDrafts = ref([])
const albumForm = reactive({ name: '', coverUrl: '', intro: '', releaseDate: '' })

const statsPeriod = ref('7')
const statsLoading = ref(false)
const statsLoaded = ref(false)
const summaryError = ref('')
const trendLoading = ref(false)
const trendError = ref('')
const trendLimited = ref(false)
const rankLoading = ref(false)
const rankError = ref('')
const regionLoading = ref(false)
const regionError = ref('')
const stats = ref({})
const trend = ref([])
const rankWorks = ref([])
const regionRowsRaw = ref([])
const regionRows = computed(() => regionRowsRaw.value)

const income = ref({})
const incomes = ref([])
const incomeTotal = ref(0)
const withdraws = ref([])
const withdrawTotal = ref(0)
const incomeLoading = ref(false)
const incomeLoaded = ref(false)
const incomeError = ref('')
const settling = ref(false)
const withdrawDialog = ref(false)
const withdrawSubmitting = ref(false)
const withdrawError = ref('')
const withdrawForm = reactive({ amount: null, account: '', payType: 'ALIPAY' })
const singerInfo = ref(null)
const singerLoading = ref(false)
const singerLoaded = ref(false)
const singerError = ref('')
const singerSubmitting = ref(false)
const singerForm = reactive({ region: '国内', gender: '男', intro: '' })
const mvs = ref([])
const mvTotal = ref(0)
const mvLoading = ref(false)
const mvLoaded = ref(false)
const mvError = ref('')
const mvDialog = ref(false)
const mvEditId = ref(null)
const mvSubmitting = ref(false)
const mvFormError = ref('')
const mvForm = reactive({
  name: '', songId: null, videoUrl: '', coverUrl: '', duration: 0, quality: 'HD', vipOnly: false, description: ''
})
const canPublishMv = computed(() => musician.value?.status === 'APPROVED' || singerInfo.value?.status === 1)
const mvSongOptions = computed(() => (works.value || [])
  .filter((work) => work.songId && work.status === 'APPROVED')
  .map((work) => ({ id: work.songId, name: work.name })))
const withdrawAccountPlaceholder = computed(() => withdrawForm.payType === 'WECHAT' ? '请输入微信号' : '请输入支付宝账号（手机号 / 邮箱）')

const statusFilterOptions = computed(() => statusFilters.map((item) => ({
  ...item,
  count: item.value ? works.value.filter((work) => work.status === item.value).length : works.value.length
})))

const filteredWorks = computed(() => {
  const keyword = workKeyword.value.trim().toLowerCase()
  const filtered = works.value.filter((work) => {
    const matchesStatus = !workStatus.value || work.status === workStatus.value
    const haystack = [work.name, work.albumName, work.style, work.language].filter(Boolean).join(' ').toLowerCase()
    return matchesStatus && (!keyword || haystack.includes(keyword))
  })
  const sorters = {
    updated_desc: (a, b) => dateValue(b.updateTime || b.createTime) - dateValue(a.updateTime || a.createTime),
    created_desc: (a, b) => dateValue(b.createTime) - dateValue(a.createTime),
    play_desc: (a, b) => Number(b.playCount || 0) - Number(a.playCount || 0),
    collect_desc: (a, b) => Number(b.collectCount || 0) - Number(a.collectCount || 0),
    like_desc: (a, b) => Number(b.likeCount || 0) - Number(a.likeCount || 0)
  }
  return filtered.slice().sort(sorters[workSort.value] || sorters.updated_desc)
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredWorks.value.length / pageSize)))
const pagedWorks = computed(() => filteredWorks.value.slice((workPage.value - 1) * pageSize, workPage.value * pageSize))
const pageStart = computed(() => (filteredWorks.value.length ? (workPage.value - 1) * pageSize + 1 : 0))
const pageEnd = computed(() => Math.min(workPage.value * pageSize, filteredWorks.value.length))
const selectedCount = computed(() => selectedIds.value.length)
const selectedWorks = computed(() => works.value.filter((work) => selectedIds.value.includes(String(work.id))))
const selectedIdSet = computed(() => new Set(selectedIds.value.map(String)))
const pageIdSet = computed(() => new Set(pagedWorks.value.map((work) => String(work.id))))
const allPageSelected = computed(() => pagedWorks.value.length > 0 && pagedWorks.value.every((work) => selectedIdSet.value.has(String(work.id))))
const somePageSelected = computed(() => pagedWorks.value.some((work) => selectedIdSet.value.has(String(work.id))))
const batchOfflineAllowed = computed(() => selectedWorks.value.length > 0 && selectedWorks.value.every((work) => work.status === 'APPROVED'))
const pendingCount = computed(() => works.value.filter((work) => work.status === 'PENDING').length)
const pendingWorks = computed(() => works.value.filter((work) => work.status === 'PENDING').slice(0, 2))
const audioReadyCount = computed(() => works.value.filter((work) => !!work.audioUrl).length)
const coverReadyCount = computed(() => works.value.filter((work) => !!work.coverUrl).length)
const lyricReadyCount = computed(() => works.value.filter((work) => !!work.lyricUrl).length)
const missingLyricWorks = computed(() => works.value.filter((work) => !work.lyricUrl))
const completeCount = computed(() => works.value.filter((work) => work.audioUrl && work.coverUrl && work.lyricUrl).length)

const albumCards = computed(() => [
  ...localAlbumDrafts.value.map((draft) => ({ ...draft, __draft: true })),
  ...albums.value
])
const albumWorkOptions = computed(() => {
  const keyword = albumWorkKeyword.value.trim().toLowerCase()
  return works.value.filter((work) => !keyword || [work.name, work.albumName].filter(Boolean).join(' ').toLowerCase().includes(keyword))
})

const collectTotal = computed(() => {
  if (worksError.value) return null
  return works.value.reduce((sum, work) => sum + Number(work.collectCount || 0), 0)
})
const statCards = computed(() => {
  const failed = !!summaryError.value
  return [
    { label: '播放量', value: failed ? '—' : formatNum(stats.value.totalPlay), note: failed ? '加载失败' : '累计' },
    { label: '收藏', value: collectTotal.value === null ? '—' : formatNum(collectTotal.value), note: worksError.value ? '作品数据不可用' : '作品汇总' },
    { label: '点赞', value: failed ? '—' : formatNum(stats.value.totalLike), note: failed ? '加载失败' : '累计' },
    { label: '评论', value: failed ? '—' : formatNum(stats.value.totalComment), note: failed ? '加载失败' : '累计' },
    { label: '粉丝记录', value: failed ? '—' : formatNum(stats.value.fans), note: failed ? '加载失败' : '关注关系' },
    { label: '粉丝增长', value: '—', note: '暂无趋势接口', muted: true }
  ]
})
const statsPeriodLabel = computed(() => periods.find((item) => item.value === statsPeriod.value)?.label || '')
const trendTotal = computed(() => trend.value.reduce((sum, item) => sum + Number(item.playCount || 0), 0))
const rankMaxPlay = computed(() => Math.max(1, ...rankWorks.value.map((item) => Number(item.playCount || 0))))
const regionTotal = computed(() => regionRowsRaw.value.reduce((sum, item) => sum + Number(item.value || 0), 0))
const regionMax = computed(() => Math.max(1, ...regionRowsRaw.value.map((item) => Number(item.value || 0))))
const latestSettlement = computed(() => incomes.value.find((item) => ['PLAY', 'GIFT'].includes(item.type)) || null)
const pendingWithdrawals = computed(() => withdraws.value.filter((item) => item.status === 'PENDING'))
const pendingWithdrawAmount = computed(() => pendingWithdrawals.value.reduce((sum, item) => sum + Number(item.amount || 0), 0))
const pendingWithdrawCount = computed(() => pendingWithdrawals.value.length)
const canWithdraw = computed(() => Number(income.value.balance || 0) > 0)
const singerStateText = computed(() => {
  if (musician.value?.status !== 'APPROVED') return '需要音乐人认证'
  if (!singerInfo.value) return '未申请'
  return { 0: '审核中', 1: '已认证', 2: '未通过' }[singerInfo.value.status] || '未申请'
})
const singerStateClass = computed(() => {
  if (musician.value?.status !== 'APPROVED') return 'is-neutral'
  if (singerInfo.value?.status === 1) return 'is-positive'
  if (singerInfo.value?.status === 0) return 'is-warning'
  if (singerInfo.value?.status === 2) return 'is-negative'
  return 'is-neutral'
})

const trendOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: false,
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#17131d',
    borderColor: '#3a3143',
    borderWidth: 1,
    textStyle: { color: '#f7f4fc', fontSize: 12 },
    valueFormatter: (value) => `${formatNum(value)} 次`
  },
  grid: { left: 54, right: 18, top: 22, bottom: 34 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => item.statDate),
    axisLine: { lineStyle: { color: '#332d3b' } },
    axisTick: { show: false },
    axisLabel: { color: '#948c9f', fontSize: 11, hideOverlap: true }
  },
  yAxis: {
    type: 'value',
    min: 0,
    minInterval: 1,
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#948c9f', fontSize: 11, formatter: (value) => formatNum(value) },
    splitLine: { lineStyle: { color: '#25212c', width: 1 } }
  },
  series: [{
    name: '播放量',
    type: 'line',
    data: trend.value.map((item) => Number(item.playCount || 0)),
    smooth: false,
    symbol: 'circle',
    symbolSize: 6,
    showSymbol: trend.value.length < 14,
    lineStyle: { width: 2, color: '#e879f9' },
    itemStyle: { color: '#e879f9' },
    areaStyle: { color: 'rgba(232,121,249,.10)' }
  }]
}))

const workDraftKey = computed(() => `km_musician_work_draft_${userStore.user?.userId || 'guest'}`)
const albumDraftKey = computed(() => `km_musician_album_drafts_${userStore.user?.userId || 'guest'}`)

function createEmptyWork() {
  return {
    name: '',
    albumId: '',
    albumName: '',
    audioUrl: '',
    coverUrl: '',
    lyricUrl: '',
    duration: 0,
    language: '国语',
    style: '民谣',
    vipOnly: false,
    releaseDate: ''
  }
}

function dateValue(value) {
  const date = new Date(value || 0).getTime()
  return Number.isFinite(date) ? date : 0
}

function formatNum(value) {
  if (value === null || value === undefined || value === '') return '—'
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  if (Math.abs(number) >= 100000000) return `${(number / 100000000).toFixed(1)}亿`
  if (Math.abs(number) >= 10000) return `${(number / 10000).toFixed(1)}万`
  return number.toLocaleString('zh-CN')
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—'
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatDuration(seconds) {
  if (!seconds || Number(seconds) <= 0) return '待读取'
  const total = Math.round(Number(seconds))
  const minutes = Math.floor(total / 60)
  const rest = total % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function workStatusText(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过', OFFLINE: '已下架' }[status] || status || '未知'
}

function workStatusClass(status) {
  return {
    PENDING: 'is-warning',
    APPROVED: 'is-positive',
    REJECTED: 'is-negative',
    OFFLINE: 'is-neutral'
  }[status] || 'is-neutral'
}

function albumStatusText(album) {
  if (album.__draft) return '草稿'
  return Number(album.status) === 1 ? '已发布' : '草稿'
}

function incomeTypeText(type) {
  return { PLAY: '播放收益', GIFT: '礼物收益', WITHDRAW: '提现' }[type] || type || '收益'
}

function incomeStatusText(status) {
  return { SUCCESS: '已结算', PENDING: '待结算', FAILED: '结算失败', REJECTED: '已拒绝' }[status] || status || '未知'
}

function incomeStatusClass(status) {
  return { SUCCESS: 'is-positive', PENDING: 'is-warning', FAILED: 'is-negative', REJECTED: 'is-negative' }[status] || 'is-neutral'
}

function withdrawStatusText(status) {
  return { PENDING: '审核中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status || '未知'
}

function withdrawStatusClass(status) {
  return { PENDING: 'is-warning', APPROVED: 'is-positive', REJECTED: 'is-negative' }[status] || 'is-neutral'
}

function withdrawMethodLabel(type) {
  if (type === 'WECHAT') return '微信'
  if (type === 'ALIPAY') return '支付宝'
  return '历史方式'
}

function selectWithdrawMethod(type) {
  withdrawForm.payType = type
  withdrawError.value = ''
}

function maskAccount(account) {
  if (!account) return '未填写收款账户'
  if (account.length <= 4) return account
  return `${account.slice(0, 2)}${'*'.repeat(Math.max(2, account.length - 6))}${account.slice(-4)}`
}

function onAvatarError(event) {
  event.target.src = fallbackAvatar
}

function onCoverError(event) {
  event.target.src = fallbackCover
}

function isSelected(id) {
  return selectedIdSet.value.has(String(id))
}

function toggleSelection(id) {
  const value = String(id)
  selectedIds.value = selectedIds.value.includes(value)
    ? selectedIds.value.filter((item) => item !== value)
    : [...selectedIds.value, value]
}

function togglePageSelection() {
  const ids = pagedWorks.value.map((work) => String(work.id))
  if (allPageSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => !ids.includes(id))
  } else {
    selectedIds.value = Array.from(new Set([...selectedIds.value, ...ids]))
  }
}

function clearSelection() {
  selectedIds.value = []
}

function setWorkStatus(value) {
  workStatus.value = value
  workPage.value = 1
  selectedIds.value = []
}

function clearFilters() {
  workKeyword.value = ''
  workStatus.value = ''
  workPage.value = 1
  selectedIds.value = []
}

function switchTab(name) {
  tab.value = name
  if (name === 'stats' && !statsLoaded.value) loadStats()
  if (name === 'income' && !incomeLoaded.value) loadIncome()
  if (name === 'singer' && !singerLoaded.value) loadSinger()
  if (name === 'mvs') {
    if (!singerLoaded.value) loadSinger()
    if (!mvLoaded.value) loadMvs()
  }
}

function onTabKeydown(event, index) {
  const keys = ['ArrowRight', 'ArrowLeft', 'Home', 'End']
  if (!keys.includes(event.key)) return
  event.preventDefault()
  let nextIndex = index
  if (event.key === 'ArrowRight') nextIndex = (index + 1) % tabs.length
  if (event.key === 'ArrowLeft') nextIndex = (index - 1 + tabs.length) % tabs.length
  if (event.key === 'Home') nextIndex = 0
  if (event.key === 'End') nextIndex = tabs.length - 1
  switchTab(tabs[nextIndex].name)
  nextTick(() => document.getElementById(`musician-tab-${tabs[nextIndex].name}`)?.focus())
}

function goToHomepage() {
  if (musician.value?.id) router.push(`/musician/${musician.value.id}`)
}

async function loadProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    const res = await musicianApi.mine()
    musician.value = res.data || null
  } catch (error) {
    profileError.value = error?.message || '音乐人资料加载失败'
  } finally {
    profileLoading.value = false
  }
}

async function loadAlbums() {
  albumsLoading.value = true
  albumsError.value = ''
  try {
    const res = await musicianApi.albums()
    albums.value = res.data || []
  } catch (error) {
    albumsError.value = error?.message || '专辑加载失败'
  } finally {
    albumsLoading.value = false
  }
}

async function fetchAllWorks() {
  const first = await musicianApi.works({ pageNum: 1, pageSize: 100 })
  let records = first.data?.records || []
  const total = Number(first.data?.total) || records.length
  let page = 2
  while (records.length < total && page <= 20) {
    const next = await musicianApi.works({ pageNum: page, pageSize: 100 })
    const nextRecords = next.data?.records || []
    if (!nextRecords.length) break
    records = records.concat(nextRecords)
    page += 1
  }
  return records
}

async function loadWorks() {
  worksLoading.value = true
  worksError.value = ''
  workActionError.value = ''
  try {
    works.value = await fetchAllWorks()
    if (workPage.value > pageCount.value) workPage.value = pageCount.value
  } catch (error) {
    worksError.value = error?.message || '作品加载失败'
  } finally {
    worksLoading.value = false
  }
}

function resetWorkForm() {
  Object.assign(workForm, createEmptyWork())
  editingWorkId.value = null
  submitted.value = false
  workFormError.value = ''
  copyrightAccepted.value = false
  lyricText.value = ''
  lyricDirty.value = false
}

function openWorkEditor(row = null) {
  resetWorkForm()
  if (row) {
    editingWorkId.value = row.id
    Object.assign(workForm, {
      name: row.name || '',
      albumId: row.albumId ? String(row.albumId) : '',
      albumName: row.albumName || '',
      audioUrl: row.audioUrl || '',
      coverUrl: row.coverUrl || '',
      lyricUrl: row.lyricUrl || '',
      duration: Number(row.duration || 0),
      language: row.language || '国语',
      style: row.style || '民谣',
      vipOnly: !!row.vipOnly,
      releaseDate: row.releaseDate || ''
    })
    if (row.lyricUrl) loadLyricText(row.lyricUrl)
  }
  workDrawer.value = true
}

async function loadLyricText(url) {
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error('lyrics unavailable')
    const text = await response.text()
    if (text.length > 60000) return
    lyricText.value = text
    lyricDirty.value = false
  } catch {
    lyricText.value = ''
    lyricDirty.value = false
  }
}

function onWorkAlbumChange(value) {
  const album = albums.value.find((item) => String(item.id) === String(value))
  workForm.albumName = album?.name || ''
}

function onAudioUploaded({ file, data }) {
  workForm.audioUrl = data?.url || workForm.audioUrl
  if (!workForm.name && file?.name) {
    workForm.name = file.name.replace(/\.[^.]+$/, '')
  }
  const audio = new Audio()
  audio.preload = 'metadata'
  audio.onloadedmetadata = () => {
    if (Number.isFinite(audio.duration)) workForm.duration = Math.round(audio.duration)
  }
  audio.src = data?.url || workForm.audioUrl
}

async function onLyricUploaded({ file }) {
  if (!file) return
  try {
    lyricText.value = await file.text()
    lyricDirty.value = true
  } catch {
    ElMessage.warning('歌词文件已上传，但无法读取文本内容')
  }
}

function resetWorkEditor() {
  if (!workSaving.value) resetWorkForm()
}

function saveWorkDraft() {
  if (!workForm.name.trim() && !workForm.audioUrl) {
    workFormError.value = '至少填写作品名称或上传音频后再保存草稿。'
    return
  }
  const draft = {
    workId: editingWorkId.value,
    savedAt: new Date().toISOString(),
    form: { ...workForm },
    lyricText: lyricText.value
  }
  try {
    localStorage.setItem(workDraftKey.value, JSON.stringify(draft))
    workDraft.value = draft
    workFormError.value = ''
    ElMessage.success('草稿已保存到本机浏览器')
  } catch {
    workFormError.value = '草稿保存失败，浏览器存储不可用。'
  }
}

function resumeWorkDraft() {
  if (!workDraft.value) return
  resetWorkForm()
  editingWorkId.value = workDraft.value.workId || null
  Object.assign(workForm, { ...createEmptyWork(), ...(workDraft.value.form || {}) })
  lyricText.value = workDraft.value.lyricText || ''
  lyricDirty.value = !!lyricText.value
  workDrawer.value = true
}

async function discardWorkDraft() {
  try {
    await ElMessageBox.confirm('删除后本机草稿无法恢复，是否继续？', '删除草稿', { type: 'warning' })
    localStorage.removeItem(workDraftKey.value)
    workDraft.value = null
    ElMessage.success('草稿已删除')
  } catch {
    /* cancelled */
  }
}

async function uploadLyricText() {
  const blob = new Blob([lyricText.value], { type: 'text/plain;charset=utf-8' })
  const safeName = (workForm.name || 'lyrics').replace(/[\\/:*?"<>|]/g, '-')
  const file = new File([blob], `${safeName}.lrc`, { type: 'text/plain' })
  const res = await fileApi.upload(file, 'lyric')
  const url = res.data?.url
  if (!url) throw new Error('歌词上传失败')
  workForm.lyricUrl = url
  return url
}

async function submitWork() {
  submitted.value = true
  workFormError.value = ''
  const errors = []
  if (!workForm.name.trim()) errors.push('请填写作品名称')
  if (!workForm.audioUrl) errors.push('请上传音频文件')
  if (!workForm.coverUrl) errors.push('请上传封面')
  if (!copyrightAccepted.value) errors.push('请确认版权声明')
  if (errors.length) {
    workFormError.value = errors.join('；')
    nextTick(() => {
      if (!workForm.name.trim()) document.getElementById('work-name')?.focus()
      else if (!copyrightAccepted.value) document.getElementById('work-copyright')?.focus()
    })
    return
  }

  workSaving.value = true
  try {
    if (lyricText.value.trim() && (lyricDirty.value || !workForm.lyricUrl)) {
      await uploadLyricText()
    } else if (lyricDirty.value && !lyricText.value.trim()) {
      workForm.lyricUrl = ''
    }
    const payload = {
      name: workForm.name.trim(),
      albumId: workForm.albumId || null,
      albumName: workForm.albumName || '',
      coverUrl: workForm.coverUrl,
      audioUrl: workForm.audioUrl,
      lyricUrl: workForm.lyricUrl,
      duration: Number(workForm.duration) || 0,
      language: workForm.language,
      style: workForm.style,
      vipOnly: !!workForm.vipOnly,
      releaseDate: workForm.releaseDate || null
    }
    if (editingWorkId.value) {
      await musicianApi.updateWork(editingWorkId.value, payload)
    } else {
      await musicianApi.createWork(payload)
    }
    await loadWorks()
    const updated = works.value.find((item) => String(item.id) === String(editingWorkId.value))
    if (editingWorkId.value && updated?.status !== 'PENDING') {
      ElMessage.warning(`修改已保存，作品当前仍为“${workStatusText(updated?.status)}”；重新送审需平台开放接口`)
    } else {
      ElMessage.success(editingWorkId.value ? '作品已保存并提交审核' : '作品已提交审核')
    }
    localStorage.removeItem(workDraftKey.value)
    workDraft.value = null
    workDrawer.value = false
    await loadProfile()
  } catch (error) {
    workFormError.value = error?.response?.data?.message || error?.message || '提交失败，请检查网络后重试'
  } finally {
    workSaving.value = false
  }
}

async function offlineWork(row) {
  try {
    await ElMessageBox.confirm(`下架《${row.name}》后，听众将无法在曲库播放，是否继续？`, '下架作品', {
      type: 'warning',
      confirmButtonText: '确认下架',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await musicianApi.offlineWork(row.id)
    ElMessage.success('作品已下架')
    await loadWorks()
  } catch {
    workActionError.value = '下架失败，后端未返回成功结果，请稍后重试。'
  }
}

async function removeWork(row) {
  try {
    await ElMessageBox.confirm(`删除《${row.name}》后不可恢复，是否继续？`, '删除作品', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await musicianApi.deleteWork(row.id)
    ElMessage.success('作品已删除')
    selectedIds.value = selectedIds.value.filter((id) => id !== String(row.id))
    await loadWorks()
  } catch {
    workActionError.value = '删除失败，作品未删除，请稍后重试。'
  }
}

async function batchOffline() {
  if (!batchOfflineAllowed.value) {
    workActionError.value = '仅已通过的作品可以批量下架，请调整选择。'
    return
  }
  try {
    await ElMessageBox.confirm(`确认下架选中的 ${selectedCount.value} 首作品？`, '批量下架', { type: 'warning' })
  } catch {
    return
  }
  const ids = selectedWorks.value.map((work) => work.id)
  const results = await Promise.allSettled(ids.map((id) => musicianApi.offlineWork(id)))
  const failed = results.filter((result) => result.status === 'rejected').length
  if (failed) {
    workActionError.value = `${ids.length - failed} 首已下架，${failed} 首失败，请刷新后重试。`
  } else {
    ElMessage.success(`已下架 ${ids.length} 首作品`)
  }
  selectedIds.value = []
  await loadWorks()
}

async function batchDelete() {
  try {
    await ElMessageBox.confirm(
      `删除选中的 ${selectedCount.value} 首作品后不可恢复，是否继续？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const ids = selectedWorks.value.map((work) => work.id)
  const results = await Promise.allSettled(ids.map((id) => musicianApi.deleteWork(id)))
  const failed = results.filter((result) => result.status === 'rejected').length
  if (failed) {
    workActionError.value = `${ids.length - failed} 首已删除，${failed} 首失败，请刷新后重试。`
  } else {
    ElMessage.success(`已删除 ${ids.length} 首作品`)
  }
  selectedIds.value = []
  await loadWorks()
}

function mvAuditText(mv) {
  if (mv.auditStatus === 'PENDING') return '审核中'
  if (mv.auditStatus === 'REJECTED') return '未通过'
  return mv.status === 1 ? '已上架' : '已下架'
}

function mvAuditClass(mv) {
  if (mv.auditStatus === 'PENDING') return 'is-warning'
  if (mv.auditStatus === 'REJECTED') return 'is-negative'
  return mv.status === 1 ? 'is-positive' : 'is-neutral'
}

async function loadMvs() {
  if (mvLoading.value) return
  mvLoading.value = true
  mvError.value = ''
  try {
    const res = await musicApi.myMvs({ pageNum: 1, pageSize: 50 })
    mvs.value = res.data?.records || []
    mvTotal.value = res.data?.total || mvs.value.length
    mvLoaded.value = true
  } catch (error) {
    mvError.value = error?.response?.data?.message || error?.message || 'MV 列表加载失败。'
  } finally {
    mvLoading.value = false
  }
}

function openMvDialog() {
  if (!canPublishMv.value) {
    ElMessage.warning('完成音乐人认证后才能发布 MV')
    return
  }
  mvFormError.value = ''
  mvEditId.value = null
  Object.assign(mvForm, { name: '', songId: null, videoUrl: '', coverUrl: '', duration: 0, quality: 'HD', vipOnly: false, description: '' })
  mvDialog.value = true
}

function openMvEdit(mv) {
  mvFormError.value = ''
  mvEditId.value = mv.id
  Object.assign(mvForm, {
    name: mv.name || '',
    songId: mv.songId || null,
    videoUrl: mv.videoUrl || '',
    coverUrl: mv.coverUrl || '',
    duration: mv.duration || 0,
    quality: mv.quality || 'HD',
    vipOnly: !!mv.vipOnly,
    description: mv.description || ''
  })
  mvDialog.value = true
}

async function setMvStatus(mv, status) {
  try {
    await musicApi.setMyMvStatus(mv.id, status)
    ElMessage.success(status === 1 ? 'MV 已上架' : 'MV 已下架')
    await loadMvs()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '操作失败')
  }
}

async function removeMv(mv) {
  try {
    await ElMessageBox.confirm(`删除 MV《${mv.name}》后不可恢复，是否继续？`, '删除 MV', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await musicApi.deleteMyMv(mv.id)
    ElMessage.success('MV 已删除')
    await loadMvs()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

async function toggleAlbumStatus(album) {
  const next = album.status === 1 ? 0 : 1
  try {
    await musicianApi.setAlbumStatus(album.id, next)
    ElMessage.success(next === 1 ? '专辑已上架' : '专辑已下架')
    await loadAlbums()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '操作失败')
  }
}
async function submitMv() {
  mvFormError.value = ''
  if (!mvForm.name.trim()) {
    mvFormError.value = '请填写 MV 名称。'
    return
  }
  if (!mvForm.videoUrl) {
    mvFormError.value = '请上传 MV 视频。'
    return
  }
  mvSubmitting.value = true
  try {
    const payload = {
      name: mvForm.name.trim(),
      songId: mvForm.songId || null,
      videoUrl: mvForm.videoUrl,
      coverUrl: mvForm.coverUrl,
      duration: Number(mvForm.duration) || 0,
      quality: mvForm.quality,
      vipOnly: mvForm.vipOnly,
      description: mvForm.description.trim()
    }
    if (mvEditId.value) {
      await musicApi.updateMyMv(mvEditId.value, payload)
      ElMessage.success('MV 已保存，修改后需重新审核')
    } else {
      await musicApi.publishMv(payload)
      ElMessage.success('MV 已提交审核')
    }
    mvDialog.value = false
    mvEditId.value = null
    await loadMvs()
    await loadMvs()
  } catch (error) {
    mvFormError.value = error?.response?.data?.message || error?.message || 'MV 提交失败，请稍后重试。'
  } finally {
    mvSubmitting.value = false
  }
}

async function loadSinger(force = false) {
  if (singerLoading.value || (singerLoaded.value && !force)) return
  singerLoading.value = true
  singerError.value = ''
  try {
    const res = await musicApi.mySinger()
    singerInfo.value = res.data || null
    if (singerInfo.value) {
      singerForm.region = singerInfo.value.region || '国内'
      singerForm.gender = singerInfo.value.gender || '男'
      singerForm.intro = singerInfo.value.intro || ''
    } else {
      singerForm.intro = musician.value?.intro || ''
    }
    singerLoaded.value = true
  } catch (error) {
    singerError.value = error?.response?.data?.message || error?.message || '歌手认证信息加载失败。'
  } finally {
    singerLoading.value = false
  }
}

async function submitSingerApply() {
  singerError.value = ''
  if (musician.value?.status !== 'APPROVED') {
    singerError.value = '只有已认证音乐人才能申请歌手认证。'
    return
  }
  if (!singerForm.region) {
    singerError.value = '请选择歌手地区。'
    return
  }
  singerSubmitting.value = true
  try {
    await musicApi.applySinger({ region: singerForm.region, gender: singerForm.gender, intro: singerForm.intro.trim() })
    ElMessage.success('歌手认证申请已提交，等待平台审核')
    await loadSinger(true)
  } catch (error) {
    singerError.value = error?.response?.data?.message || error?.message || '歌手认证提交失败，请稍后重试。'
  } finally {
    singerSubmitting.value = false
  }
}

function openWithdraw() {
  if (!canWithdraw.value) return
  withdrawError.value = ''
  withdrawForm.amount = null
  withdrawForm.account = ''
  withdrawForm.payType = 'ALIPAY'
  withdrawDialog.value = true
}

async function submitWithdraw() {
  withdrawError.value = ''
  const amount = Number(withdrawForm.amount)
  if (!Number.isInteger(amount) || amount < 1) {
    withdrawError.value = '请输入不少于 ¥1 的整数提现金额。'
    return
  }
  if (amount > Number(income.value.balance || 0)) {
    withdrawError.value = '提现金额不能超过可提现余额。'
    return
  }
  if (!['ALIPAY', 'WECHAT'].includes(withdrawForm.payType)) {
    withdrawError.value = '请选择支付宝或微信提现。'
    return
  }
  if (!withdrawForm.account.trim()) {
    withdrawError.value = withdrawForm.payType === 'WECHAT' ? '请填写微信号。' : '请填写支付宝账号。'
    return
  }
  withdrawSubmitting.value = true
  try {
    await musicianApi.withdraw(amount, withdrawForm.account.trim(), withdrawForm.payType)
    ElMessage.success('提现申请已提交，等待平台审核')
    withdrawDialog.value = false
    await loadIncome()
  } catch (error) {
    withdrawError.value = error?.response?.data?.message || error?.message || '提现申请提交失败，请稍后重试。'
  } finally {
    withdrawSubmitting.value = false
  }
}

function openAlbumEditor(album = null) {
  albumFormError.value = ''
  albumWorkKeyword.value = ''
  if (!album) {
    albumEditSource.value = 'new'
    albumEditId.value = null
    albumEditOriginal.value = null
    Object.assign(albumForm, { name: '', coverUrl: '', intro: '', releaseDate: '' })
    selectedWorkIds.value = []
  } else if (album.__draft) {
    albumEditSource.value = 'draft'
    albumEditId.value = album.localId
    albumEditOriginal.value = null
    Object.assign(albumForm, { name: album.name || '', coverUrl: album.coverUrl || '', intro: album.intro || '', releaseDate: album.releaseDate || '' })
    selectedWorkIds.value = (album.workIds || []).map(String)
  } else {
    albumEditSource.value = 'remote'
    albumEditId.value = album.id
    albumEditOriginal.value = album
    Object.assign(albumForm, {
      name: album.name || '',
      coverUrl: album.coverUrl || '',
      intro: album.intro || '',
      releaseDate: album.releaseDate || ''
    })
    selectedWorkIds.value = works.value.filter((work) => workBelongsToAlbum(work, album)).map((work) => String(work.id))
  }
  albumDialog.value = true
}

function toggleAlbumWork(id) {
  const value = String(id)
  selectedWorkIds.value = selectedWorkIds.value.includes(value)
    ? selectedWorkIds.value.filter((item) => item !== value)
    : [...selectedWorkIds.value, value]
}

function saveAlbumDraft() {
  if (!albumForm.name.trim()) {
    albumFormError.value = '请填写专辑名称后再保存草稿。'
    return
  }
  const draft = {
    localId: albumEditId.value || `local-${Date.now()}`,
    name: albumForm.name.trim(),
    coverUrl: albumForm.coverUrl,
    intro: albumForm.intro,
    releaseDate: albumForm.releaseDate,
    workIds: [...selectedWorkIds.value],
    savedAt: new Date().toISOString()
  }
  const drafts = localAlbumDrafts.value.filter((item) => item.localId !== draft.localId)
  localAlbumDrafts.value = [draft, ...drafts]
  localStorage.setItem(albumDraftKey.value, JSON.stringify(localAlbumDrafts.value))
  albumDialog.value = false
  ElMessage.success('专辑草稿已保存到本机')
}

function workBelongsToAlbum(work, album) {
  if (!album) return false
  if (work.albumId && String(work.albumId) === String(album.id)) return true
  return !work.albumId && album.name && work.albumName === album.name
}

function albumSongCount(album) {
  if (album.__draft) return (album.workIds || []).filter((id) => works.value.some((work) => String(work.id) === String(id))).length
  const matched = works.value.filter((work) => workBelongsToAlbum(work, album)).length
  if (!works.value.length) return Number(album.songCount || 0)
  return matched
}

function serializedWork(work, albumId, albumName) {
  return {
    name: work.name,
    albumId,
    albumName: albumName || '',
    coverUrl: work.coverUrl || '',
    audioUrl: work.audioUrl || '',
    lyricUrl: work.lyricUrl || '',
    duration: Number(work.duration || 0),
    language: work.language || '国语',
    style: work.style || '民谣',
    vipOnly: !!work.vipOnly,
    releaseDate: work.releaseDate || null
  }
}

async function publishAlbum() {
  if (!albumForm.name.trim()) {
    albumFormError.value = '请填写专辑名称。'
    nextTick(() => document.getElementById('album-name')?.focus())
    return
  }
  const currentMembers = albumEditSource.value === 'remote' && albumEditOriginal.value
    ? works.value.filter((work) => workBelongsToAlbum(work, albumEditOriginal.value))
    : []
  const selectedSet = new Set(selectedWorkIds.value)
  const selected = works.value.filter((work) => selectedSet.has(String(work.id)))
  const removed = currentMembers.filter((work) => !selectedSet.has(String(work.id)))
  const changed = [...selected, ...removed]
  const requiresReaudit = changed.some((work) => work.status === 'APPROVED' || work.status === 'PENDING')
  if (requiresReaudit) {
    try {
      await ElMessageBox.confirm(
        '加入或移除已通过/待审核作品会触发重新审核，相关作品可能暂时离开曲库。是否继续发布？',
        '确认调整曲目',
        { type: 'warning', confirmButtonText: '继续发布', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }

  albumSaving.value = true
  albumFormError.value = ''
  try {
    let albumId = albumEditId.value
    if (albumEditSource.value === 'remote') {
      await musicianApi.updateAlbum(albumEditId.value, {
        name: albumForm.name.trim(),
        coverUrl: albumForm.coverUrl,
        intro: albumForm.intro,
        releaseDate: albumForm.releaseDate || null
      })
    } else {
      const res = await musicianApi.createAlbum({
        name: albumForm.name.trim(),
        coverUrl: albumForm.coverUrl,
        intro: albumForm.intro,
        releaseDate: albumForm.releaseDate || null
      })
      albumId = res.data
    }

    const requests = []
    for (const work of selected) {
      if (String(work.albumId) !== String(albumId) || work.albumName !== albumForm.name.trim()) {
        requests.push(musicianApi.updateWork(work.id, serializedWork(work, albumId, albumForm.name.trim())))
      }
    }
    for (const work of removed) {
      requests.push(musicianApi.updateWork(work.id, serializedWork(work, null, null)))
    }
    const results = await Promise.allSettled(requests)
    const failed = results.filter((result) => result.status === 'rejected').length
    if (albumEditSource.value === 'draft') {
      localAlbumDrafts.value = localAlbumDrafts.value.filter((item) => item.localId !== albumEditId.value)
      localStorage.setItem(albumDraftKey.value, JSON.stringify(localAlbumDrafts.value))
    }
    albumDialog.value = false
    await Promise.all([loadAlbums(), loadWorks()])
    if (failed) {
      albumActionError.value = `专辑信息已发布，但有 ${failed} 首作品的归属同步失败，请编辑专辑后重试。`
    } else {
      ElMessage.success(albumEditSource.value === 'remote' ? '专辑已保存并发布' : '专辑已发布')
    }
  } catch (error) {
    albumFormError.value = error?.response?.data?.message || error?.message || '专辑发布失败，请稍后重试。'
  } finally {
    albumSaving.value = false
  }
}

async function removeAlbum(album) {
  try {
    await ElMessageBox.confirm(
      album.__draft ? '删除后本机专辑草稿无法恢复，是否继续？' : `删除专辑《${album.name}》后不可恢复，是否继续？`,
      '删除专辑',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    if (album.__draft) {
      localAlbumDrafts.value = localAlbumDrafts.value.filter((item) => item.localId !== album.localId)
      localStorage.setItem(albumDraftKey.value, JSON.stringify(localAlbumDrafts.value))
    } else {
      await musicianApi.deleteAlbum(album.id)
    }
    ElMessage.success('专辑已删除')
    await loadAlbums()
  } catch {
    albumActionError.value = '专辑删除失败，请稍后重试。'
  }
}

function loadLocalDrafts() {
  try {
    const rawWork = localStorage.getItem(workDraftKey.value)
    workDraft.value = rawWork ? JSON.parse(rawWork) : null
  } catch {
    workDraft.value = null
  }
  try {
    const rawAlbums = localStorage.getItem(albumDraftKey.value)
    const parsed = rawAlbums ? JSON.parse(rawAlbums) : []
    localAlbumDrafts.value = Array.isArray(parsed) ? parsed : []
  } catch {
    localAlbumDrafts.value = []
  }
}

async function loadStats() {
  statsLoading.value = true
  trendLoading.value = statsPeriod.value !== '90'
  trendLimited.value = statsPeriod.value === '90'
  rankLoading.value = true
  regionLoading.value = true
  summaryError.value = ''
  trendError.value = ''
  rankError.value = ''
  regionError.value = ''
  if (trendLimited.value) trend.value = []
  const apiPeriod = statsPeriod.value === '7' ? 'week' : 'month'
  const tasks = [
    musicianApi.statistics(apiPeriod),
    musicianApi.rank(),
    musicianApi.fansRegions(),
    statsPeriod.value === '90' ? Promise.resolve(null) : musicianApi.trend(apiPeriod)
  ]
  try {
    const [summaryResult, rankResult, regionResult, trendResult] = await Promise.allSettled(tasks)
    if (summaryResult.status === 'fulfilled') stats.value = summaryResult.value.data || {}
    else summaryError.value = summaryResult.reason?.message || '统计数据加载失败'

    if (rankResult.status === 'fulfilled') rankWorks.value = rankResult.value.data || []
    else rankError.value = rankResult.reason?.message || '作品排行加载失败'

    if (regionResult.status === 'fulfilled') regionRowsRaw.value = regionResult.value.data || []
    else regionError.value = regionResult.reason?.message || '地域数据加载失败'

    if (trendResult.status === 'fulfilled' && trendResult.value) {
      trend.value = trendResult.value.data || []
    } else if (trendResult.status === 'rejected') {
      trendError.value = trendResult.reason?.message || '趋势数据加载失败'
    }
    statsLoaded.value = true
  } finally {
    statsLoading.value = false
    trendLoading.value = false
    rankLoading.value = false
    regionLoading.value = false
  }
}

function changeStatsPeriod(value) {
  if (statsPeriod.value === value) return
  statsPeriod.value = value
  loadStats()
}

function viewThirtyDays() {
  statsPeriod.value = '30'
  loadStats()
}

function rankBarWidth(item) {
  return Math.max(3, Math.round((Number(item.playCount || 0) / rankMaxPlay.value) * 100))
}

function regionWidth(value) {
  return Math.max(3, Math.round((Number(value || 0) / regionMax.value) * 100))
}

async function loadIncome() {
  incomeLoading.value = true
  incomeError.value = ''
  try {
    const [summaryRes, incomesRes, withdrawsRes] = await Promise.all([
      musicianApi.incomeSummary(),
      musicianApi.incomes({ pageNum: 1, pageSize: 50 }),
      musicianApi.withdraws({ pageNum: 1, pageSize: 50 })
    ])
    income.value = summaryRes.data || {}
    incomes.value = incomesRes.data?.records || []
    incomeTotal.value = incomesRes.data?.total || incomes.value.length
    withdraws.value = withdrawsRes.data?.records || []
    withdrawTotal.value = withdrawsRes.data?.total || withdraws.value.length
    incomeLoaded.value = true
  } catch (error) {
    incomeError.value = error?.message || '收益数据加载失败'
  } finally {
    incomeLoading.value = false
  }
}

async function settleIncome() {
  try {
    await ElMessageBox.confirm(
      '将按现有播放量和直播礼物重新计算可结算收益，重复结算只补差额。是否继续？',
      '结算收益',
      { type: 'warning', confirmButtonText: '开始结算', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  settling.value = true
  try {
    const res = await musicianApi.settle()
    const playIncome = Number(res.data?.playIncome || 0)
    const giftIncome = Number(res.data?.giftIncome || 0)
    if (!playIncome && !giftIncome) ElMessage.info('当前没有新的可结算收益')
    else ElMessage.success(`结算完成：播放收益 ¥${formatMoney(playIncome)}，礼物收益 ¥${formatMoney(giftIncome)}`)
    await loadIncome()
  } catch {
    incomeError.value = '结算失败，请确认直播服务与收益接口可用后重试。'
  } finally {
    settling.value = false
  }
}

watch(workKeyword, () => { workPage.value = 1 })
watch(workStatus, () => { workPage.value = 1 })
watch(workSort, () => { workPage.value = 1 })
watch(pageCount, (count) => { if (workPage.value > count) workPage.value = count })

onMounted(async () => {
  loadLocalDrafts()
  await Promise.all([loadProfile(), loadAlbums(), loadWorks()])
})
</script>

<style scoped>
.musician-console {
  --mc-ground: #08070b;
  --mc-surface: #111015;
  --mc-raised: #17141c;
  --mc-border: #25212c;
  --mc-border-strong: #3a3344;
  --mc-text: #f7f4fc;
  --mc-muted: #b0a8bb;
  --mc-faint: #948c9f;
  --mc-accent: #e9b8ff;
  --mc-accent-data: #e879f9;
  --mc-positive: #72e6a2;
  --mc-warning: #f6c177;
  --mc-negative: #ff8fa2;
  color: var(--mc-text);
  padding-bottom: 24px;
}

.artist-band {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 82px;
  padding: 14px 16px;
  border: 1px solid var(--mc-border);
  border-radius: 12px;
  background: var(--mc-surface);
}
.artist-avatar {
  width: 54px;
  height: 54px;
  flex: none;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: #211a2a;
  color: var(--mc-accent);
  font-size: 20px;
  font-weight: 600;
}
.artist-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.artist-copy { min-width: 0; }
.artist-copy h1 { margin: 0; font-size: 25px; line-height: 1.15; font-weight: 650; letter-spacing: -.035em; }
.artist-meta { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; margin-top: 6px; color: var(--mc-faint); font-size: 12px; }
.artist-meta > span + span::before { content: '·'; margin-right: 9px; color: #5f5669; }
.artist-meta .state-pill::before { display: none; }
.artist-actions { margin-left: auto; display: flex; align-items: center; gap: 8px; flex: none; }

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color .15s, border-color .15s, color .15s;
}
.button:disabled { opacity: .46; cursor: not-allowed; }
.button-primary { background: var(--mc-accent); border-color: var(--mc-accent); color: #140d1a; }
.button-primary:hover:not(:disabled) { background: #f3d2ff; border-color: #f3d2ff; }
.button-quiet { background: transparent; border-color: var(--mc-border-strong); color: var(--mc-text); }
.button-quiet:hover:not(:disabled) { background: var(--mc-raised); border-color: #4b4256; }
.button-danger { background: transparent; border-color: rgba(255, 143, 162, .42); color: var(--mc-negative); }
.button-danger:hover:not(:disabled) { background: rgba(255, 143, 162, .08); }
.button-small { height: 32px; padding: 0 12px; font-size: 12px; }
.text-button {
  border: 0;
  background: transparent;
  color: var(--mc-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 6px 4px;
}
.text-button:hover { color: var(--mc-text); }
.text-button.is-danger { color: var(--mc-negative); }
.icon-button {
  width: 30px;
  height: 30px;
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--mc-border);
  border-radius: 7px;
  background: transparent;
  color: var(--mc-faint);
  cursor: pointer;
}
.icon-button:hover { color: var(--mc-text); background: var(--mc-raised); }

.inline-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 38px;
  padding: 8px 12px;
  margin-top: 10px;
  border: 1px solid rgba(255, 143, 162, .25);
  border-radius: 8px;
  background: rgba(255, 143, 162, .06);
  color: #ffbdc8;
  font-size: 12px;
}
.inline-alert.is-warning { border-color: rgba(246, 193, 119, .24); background: rgba(246, 193, 119, .06); color: #f6d7a5; }
.inline-alert span { flex: 1; line-height: 1.5; }
.inline-alert button, .inline-alert a { border: 0; background: transparent; color: var(--mc-accent); font-weight: 600; font-size: 12px; cursor: pointer; white-space: nowrap; }

.console-tabs {
  display: flex;
  align-items: stretch;
  gap: 26px;
  margin-top: 12px;
  border-bottom: 1px solid var(--mc-border);
  overflow-x: auto;
  scrollbar-width: none;
}
.console-tabs::-webkit-scrollbar { display: none; }
.console-tab {
  position: relative;
  flex: none;
  height: 46px;
  padding: 0 1px;
  border: 0;
  background: transparent;
  color: var(--mc-faint);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
}
.console-tab:hover { color: var(--mc-text); }
.console-tab.active { color: var(--mc-text); font-weight: 600; }
.console-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--mc-accent-data);
}
.console-panel { outline: none; padding-top: 18px; }

.section-head, .panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.section-head h2 { margin: 0; font-size: 20px; line-height: 1.25; font-weight: 650; letter-spacing: -.025em; }
.section-head p { margin: 5px 0 0; color: var(--mc-faint); font-size: 12.5px; line-height: 1.55; }
.section-note { color: var(--mc-faint); font-size: 11.5px; white-space: nowrap; padding-top: 2px; }
.head-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }

.draft-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
  padding: 9px 12px;
  border: 1px solid #3b3045;
  border-radius: 8px;
  background: #16111c;
}
.draft-copy { display: flex; align-items: center; gap: 9px; min-width: 0; font-size: 12px; color: var(--mc-muted); }
.draft-copy strong { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--mc-text); font-weight: 600; }
.draft-copy > span:last-child { color: var(--mc-faint); font-size: 11px; }
.draft-mark { flex: none; padding: 2px 6px; border-radius: 4px; background: rgba(232, 121, 249, .12); color: var(--mc-accent); font-size: 10.5px; font-weight: 600; }
.draft-actions { display: flex; align-items: center; gap: 4px; flex: none; }

.work-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 14px 0 10px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 286px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--mc-border-strong);
  border-radius: 7px;
  background: var(--mc-surface);
  color: var(--mc-faint);
}
.search-box:focus-within { border-color: rgba(232, 121, 249, .55); }
.search-box input {
  width: 100%;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--mc-text);
  font-size: 12px;
}
.search-box input::placeholder { color: #766d80; }
.filter-group { display: flex; align-items: center; gap: 3px; padding: 3px; border: 1px solid var(--mc-border); border-radius: 8px; background: #0c0b10; }
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 8px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--mc-faint);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
}
.filter-chip span { color: #766d80; font-variant-numeric: tabular-nums; }
.filter-chip:hover { color: var(--mc-text); background: #19151f; }
.filter-chip.active { background: #251d2c; color: var(--mc-text); }
.filter-chip.active span { color: var(--mc-accent); }
.sort-select { display: flex; align-items: center; gap: 7px; margin-left: auto; color: var(--mc-faint); font-size: 11.5px; white-space: nowrap; }
.sort-select select {
  height: 34px;
  padding: 0 26px 0 9px;
  border: 1px solid var(--mc-border-strong);
  border-radius: 7px;
  background: var(--mc-surface);
  color: var(--mc-text);
  font-size: 12px;
  outline: none;
}
.sort-select select:focus { border-color: rgba(232, 121, 249, .55); }

.bulk-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 10px;
  padding: 8px 10px 8px 12px;
  border: 1px solid #43344f;
  border-radius: 8px;
  background: #1b1422;
}
.bulk-summary { display: flex; align-items: center; gap: 9px; min-width: 0; }
.bulk-summary strong { font-size: 12px; font-weight: 600; color: var(--mc-text); }
.bulk-summary span { color: var(--mc-faint); font-size: 11.5px; }
.bulk-actions { display: flex; align-items: center; gap: 6px; }

.table-panel { margin-top: 10px; border: 1px solid var(--mc-border); border-radius: 10px; background: var(--mc-surface); overflow: hidden; }
.table-scroll { overflow-x: auto; }
.works-table { width: 100%; border-collapse: collapse; min-width: 960px; }
.works-table th {
  height: 34px;
  padding: 0 10px;
  border-bottom: 1px solid var(--mc-border);
  color: var(--mc-faint);
  font-size: 11.5px;
  font-weight: 500;
  text-align: left;
  white-space: nowrap;
}
.works-table td { height: 62px; padding: 8px 10px; border-bottom: 1px solid #1e1a24; color: var(--mc-text); font-size: 13px; vertical-align: middle; }
.works-table tbody tr:last-child td { border-bottom: 0; }
.works-table tbody tr:hover, .works-table tbody tr.selected { background: #17131d; }
.works-table .check-cell { width: 38px; padding-left: 14px; padding-right: 4px; }
.works-table .number-cell { text-align: right; font-variant-numeric: tabular-nums; width: 66px; }
.works-table .date-cell { width: 92px; color: var(--mc-faint); font-size: 11.5px; }
.works-table .action-cell { width: 164px; }
.works-table input[type='checkbox'] { width: 14px; height: 14px; accent-color: #e879f9; cursor: pointer; }
.song-cell { display: flex; align-items: center; gap: 10px; min-width: 230px; }
.song-cell img { width: 40px; height: 40px; flex: none; border-radius: 5px; object-fit: cover; background: #221b2a; }
.song-copy { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.song-copy strong { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-copy span { color: var(--mc-faint); font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.state-pill {
  display: inline-flex;
  align-items: center;
  height: 21px;
  padding: 0 7px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}
.state-pill.is-positive { background: rgba(114, 230, 162, .10); border-color: rgba(114, 230, 162, .18); color: var(--mc-positive); }
.state-pill.is-warning { background: rgba(246, 193, 119, .10); border-color: rgba(246, 193, 119, .18); color: var(--mc-warning); }
.state-pill.is-negative { background: rgba(255, 143, 162, .10); border-color: rgba(255, 143, 162, .18); color: var(--mc-negative); }
.state-pill.is-neutral { background: #221d29; border-color: #342d3e; color: #bdb4c8; }
.state-pill.is-draft { background: rgba(232, 121, 249, .10); border-color: rgba(232, 121, 249, .18); color: var(--mc-accent); }
.reason-note { display: block; margin-top: 3px; color: #c78d9a; font-size: 10.5px; cursor: help; }
.row-actions { display: flex; align-items: center; gap: 2px; }
.row-action {
  padding: 4px 5px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #c7bfd0;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
.row-action:hover:not(:disabled) { color: var(--mc-text); background: #231d2b; }
.row-action.is-danger { color: var(--mc-negative); }
.row-action.is-danger:hover { background: rgba(255, 143, 162, .08); }
.row-action.is-disabled, .row-action:disabled { color: #6d6377; cursor: not-allowed; }

.release-checks { margin-top: 14px; padding: 16px; border: 1px solid var(--mc-border); border-radius: 9px; background: var(--mc-surface); min-height: 192px; }
.release-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.release-head h2 { margin: 0; font-size: 13.5px; font-weight: 600; }
.release-head p { margin: 4px 0 0; color: var(--mc-faint); font-size: 11.5px; }
.release-grid { display: grid; grid-template-columns: 1.05fr 0.95fr 1fr; gap: 0; }
.release-card { min-width: 0; padding: 0 16px; border-left: 1px solid #25202d; }
.release-card:first-child { padding-left: 0; border-left: 0; }
.release-card:last-child { padding-right: 0; }
.release-card-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; color: var(--mc-faint); font-size: 11px; }
.release-card-head strong { color: var(--mc-text); font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.check-list { display: flex; gap: 14px; margin: 12px 0 10px; }
.check-list div { display: flex; flex-direction: column; gap: 2px; }
.check-list dt { color: var(--mc-faint); font-size: 10.5px; }
.check-list dd { margin: 0; color: var(--mc-text); font-size: 13px; font-variant-numeric: tabular-nums; }
.release-action { padding: 4px 0; margin-top: 5px; color: var(--mc-accent); }
.release-done { display: inline-block; margin-top: 8px; color: var(--mc-positive); font-size: 11px; }
.pending-list { list-style: none; margin: 11px 0 7px; padding: 0; display: flex; flex-direction: column; gap: 6px; }
.pending-list li { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--mc-muted); font-size: 11.5px; }
.pending-list li span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-list time { flex: none; color: var(--mc-faint); font-size: 10.5px; font-variant-numeric: tabular-nums; }
.release-empty { margin: 16px 0 8px; color: var(--mc-faint); font-size: 11.5px; }
.release-rules ul { margin: 11px 0 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 7px; }
.release-rules li { position: relative; padding-left: 11px; color: var(--mc-faint); font-size: 11.5px; line-height: 1.5; }
.release-rules li::before { content: ''; position: absolute; left: 0; top: 6px; width: 3px; height: 3px; border-radius: 50%; background: #655b70; }

.table-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 12px; border-top: 1px solid var(--mc-border); color: var(--mc-faint); font-size: 11.5px; }
.pager { display: flex; align-items: center; gap: 9px; }
.pager button { height: 28px; padding: 0 9px; border: 1px solid var(--mc-border-strong); border-radius: 5px; background: transparent; color: var(--mc-muted); font-size: 11.5px; cursor: pointer; }
.pager button:hover:not(:disabled) { color: var(--mc-text); background: var(--mc-raised); }
.pager button:disabled { opacity: .38; cursor: not-allowed; }
.pager span { min-width: 42px; text-align: center; font-variant-numeric: tabular-nums; }

.work-cards { display: none; }
.work-skeleton { padding: 5px 0; }
.skeleton-row { display: flex; align-items: center; gap: 12px; height: 62px; padding: 0 14px; border-bottom: 1px solid #1e1a24; }
.skeleton { display: block; border-radius: 4px; background: #211c28; }
.skeleton-check { width: 14px; height: 14px; }
.skeleton-cover { width: 40px; height: 40px; border-radius: 5px; }
.skeleton-copy { width: 190px; height: 28px; }
.skeleton-pill { width: 52px; height: 20px; margin-left: auto; }
.skeleton-number { width: 34px; height: 15px; }
.sr-only {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}
.state-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 236px;
  padding: 26px;
  border: 1px dashed #3a3143;
  border-radius: 9px;
  margin: 14px;
  text-align: center;
}
.state-block > .el-icon { font-size: 23px; color: var(--mc-accent); }
.state-block strong { font-size: 13px; font-weight: 600; }
.state-block p { max-width: 380px; margin: 0 0 4px; color: var(--mc-faint); font-size: 11.5px; line-height: 1.6; }
.state-block.is-compact { min-height: 170px; margin: 0; border: 0; }
.state-block.is-error > .el-icon { color: var(--mc-negative); }
.state-block.is-error { border-color: rgba(255, 143, 162, .25); }

.album-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(228px, 1fr)); gap: 12px; margin-top: 14px; }
.album-card { display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--mc-border); border-radius: 10px; background: var(--mc-surface); }
.album-cover { aspect-ratio: 1; overflow: hidden; background: #1b1621; }
.album-cover img { width: 100%; height: 100%; display: block; object-fit: cover; }
.album-card-body { padding: 11px 12px 8px; }
.album-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.album-heading strong { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.album-card-body p { display: -webkit-box; min-height: 32px; margin: 6px 0; overflow: hidden; color: var(--mc-faint); font-size: 11px; line-height: 1.5; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.album-meta { display: flex; justify-content: space-between; gap: 8px; color: #81778c; font-size: 11px; }
.album-actions { display: flex; justify-content: flex-end; gap: 3px; padding: 5px 8px 8px; border-top: 1px solid #1e1a24; }
.album-card.is-skeleton { padding: 12px; gap: 10px; min-height: 280px; }
.skeleton-album-cover { width: 100%; aspect-ratio: 1; }
.skeleton-line { width: 72%; height: 14px; }
.skeleton-line.short { width: 45%; height: 11px; }

.stats-head { align-items: flex-end; }
.period-switch { display: flex; align-items: center; gap: 2px; padding: 3px; border: 1px solid var(--mc-border); border-radius: 7px; background: #0c0b10; }
.period-switch button { height: 27px; padding: 0 10px; border: 0; border-radius: 5px; background: transparent; color: var(--mc-faint); font-size: 11.5px; cursor: pointer; }
.period-switch button:hover { color: var(--mc-text); }
.period-switch button.active { background: #251d2c; color: var(--mc-text); }
.metric-strip { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); margin-top: 14px; border: 1px solid var(--mc-border); border-radius: 9px; overflow: hidden; background: var(--mc-surface); }
.metric-cell { min-width: 0; display: flex; flex-direction: column; gap: 5px; padding: 13px 14px; border-right: 1px solid var(--mc-border); }
.metric-cell:last-child { border-right: 0; }
.metric-label { color: var(--mc-faint); font-size: 12px; }
.metric-value { color: var(--mc-text); font-size: 23px; line-height: 1.2; font-weight: 600; letter-spacing: -.02em; font-variant-numeric: tabular-nums; }
.metric-value.is-muted { color: #8f8599; }
.metric-note { color: #7c7287; font-size: 10.5px; line-height: 1.4; }
.metric-skeleton { width: 76%; height: 24px; margin: 2px 0; }

.stats-grid { display: grid; grid-template-columns: minmax(0, 1.85fr) minmax(300px, 0.9fr); gap: 12px; margin-top: 12px; }
.panel { min-width: 0; border: 1px solid var(--mc-border); border-radius: 9px; background: var(--mc-surface); padding: 14px; }
.panel-head { align-items: flex-start; margin-bottom: 12px; }
.panel-head h3 { margin: 0; font-size: 13.5px; font-weight: 600; }
.panel-head p { margin: 4px 0 0; color: var(--mc-faint); font-size: 12px; }
.panel-figure { color: var(--mc-text); font-size: 20px; line-height: 1; font-weight: 600; font-variant-numeric: tabular-nums; white-space: nowrap; }
.panel-figure small { color: var(--mc-faint); font-size: 10.5px; font-weight: 400; }
.chart-skeleton { display: flex; align-items: flex-end; gap: 12px; height: 286px; padding: 20px 10px; border-bottom: 1px solid #25212c; }
.chart-skeleton .skeleton { flex: 1; background: #211c28; border-radius: 3px 3px 0 0; }
.rank-skeleton { padding-top: 3px; }
.rank-skeleton-row { display: flex; align-items: center; gap: 10px; height: 62px; border-bottom: 1px solid #1e1a24; }
.rank-skeleton-row .skeleton-number { margin-left: auto; }
.rank-list { list-style: none; margin: 0; padding: 0; }
.rank-item { display: flex; align-items: flex-start; gap: 9px; padding: 9px 0; border-bottom: 1px solid #1e1a24; }
.rank-item:last-child { border-bottom: 0; padding-bottom: 3px; }
.rank-index { width: 16px; flex: none; padding-top: 14px; color: #7d7288; font-size: 11px; font-variant-numeric: tabular-nums; text-align: center; }
.rank-item img { width: 36px; height: 36px; flex: none; border-radius: 4px; object-fit: cover; background: #211a2a; }
.rank-copy { min-width: 0; flex: 1; }
.rank-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.rank-heading strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 11.5px; font-weight: 600; }
.rank-heading span { flex: none; color: var(--mc-text); font-size: 11.5px; font-variant-numeric: tabular-nums; }
.rank-track { height: 3px; margin: 5px 0 4px; border-radius: 2px; background: #25202d; overflow: hidden; }
.rank-track span { display: block; height: 100%; background: var(--mc-accent-data); border-radius: inherit; }
.rank-meta { color: var(--mc-faint); font-size: 11px; }

.lower-grid { display: grid; grid-template-columns: minmax(0, 1.85fr) minmax(300px, 0.9fr); gap: 12px; margin-top: 12px; }
.region-list { display: flex; flex-direction: column; gap: 10px; }
.region-row { display: grid; grid-template-columns: 72px minmax(0, 1fr) 48px; align-items: center; gap: 10px; color: var(--mc-muted); font-size: 11.5px; }
.region-row strong { color: var(--mc-text); font-size: 11.5px; font-weight: 500; text-align: right; font-variant-numeric: tabular-nums; }
.region-track { height: 3px; border-radius: 2px; background: #25202d; overflow: hidden; }
.region-track span { display: block; height: 100%; border-radius: inherit; background: var(--mc-accent-data); }
.region-skeleton { display: flex; flex-direction: column; gap: 12px; }
.region-skeleton .skeleton { width: 100%; height: 10px; }
.data-notes ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 8px; }
.data-notes li { position: relative; padding-left: 12px; color: var(--mc-faint); font-size: 11.5px; line-height: 1.55; }
.data-notes li::before { content: ''; position: absolute; left: 0; top: 6px; width: 4px; height: 4px; border-radius: 50%; background: #655b70; }

.income-lead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 14px;
  padding: 16px;
  border: 1px solid var(--mc-border);
  border-radius: 9px;
  background: var(--mc-surface);
}
.balance-copy { display: flex; flex-direction: column; gap: 5px; }
.balance-copy > span { color: var(--mc-faint); font-size: 11.5px; }
.balance-copy strong { font-size: 34px; font-weight: 600; line-height: 1.1; letter-spacing: -.035em; font-variant-numeric: tabular-nums; }
.balance-copy p { margin: 0; color: var(--mc-faint); font-size: 11px; }
.settlement-state { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
.settlement-state small { color: var(--mc-faint); font-size: 10.5px; text-align: right; }
.income-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.income-grid { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.85fr); gap: 12px; margin-top: 12px; }
.income-side { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.record-list { display: flex; flex-direction: column; }
.record-row { display: grid; grid-template-columns: minmax(0, 1fr) 68px 100px 92px; align-items: center; gap: 9px; min-height: 54px; border-bottom: 1px solid #1e1a24; }
.record-row:last-child { border-bottom: 0; }
.record-copy { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.record-copy strong { font-size: 11.5px; font-weight: 600; }
.record-copy span { color: var(--mc-faint); font-size: 10.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.record-row time { color: var(--mc-faint); font-size: 10.5px; }
.record-amount { color: var(--mc-positive); font-size: 12px; font-weight: 600; text-align: right; font-variant-numeric: tabular-nums; }
.record-amount.negative { color: var(--mc-negative); }
.withdraw-list { display: flex; flex-direction: column; }
.withdraw-item { display: flex; flex-direction: column; gap: 4px; padding: 10px 0; border-bottom: 1px solid #1e1a24; }
.withdraw-item:last-child { border-bottom: 0; }
.withdraw-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.withdraw-top strong { font-size: 17px; font-weight: 600; font-variant-numeric: tabular-nums; }
.withdraw-item > span { color: var(--mc-muted); font-size: 11px; }
.withdraw-item time { color: var(--mc-faint); font-size: 10.5px; }
.withdraw-item p { margin: 0; padding: 5px 7px; border-radius: 4px; background: #19151f; color: var(--mc-faint); font-size: 10.5px; line-height: 1.5; }
.risk-note { display: flex; align-items: flex-start; gap: 9px; padding: 11px 12px; border: 1px solid rgba(246, 193, 119, .18); border-radius: 8px; background: rgba(246, 193, 119, .05); color: #e2c48f; }
.risk-note .el-icon { flex: none; margin-top: 1px; color: var(--mc-warning); }
.risk-note strong { display: block; margin-bottom: 3px; font-size: 11.5px; font-weight: 600; }
.risk-note p { margin: 0; font-size: 10.5px; line-height: 1.6; }
.risk-note.is-inline { margin-top: 12px; }
.income-skeleton { display: flex; flex-direction: column; gap: 12px; margin-top: 14px; }
.income-lead-skeleton { height: 114px; border-radius: 9px; }
.income-table-skeleton { height: 240px; border-radius: 9px; }

.editor-form { display: flex; flex-direction: column; }
.form-section { padding: 0 0 18px; margin-bottom: 18px; border-bottom: 1px solid #26212e; }
.form-section:last-of-type { border-bottom: 0; margin-bottom: 0; padding-bottom: 0; }
.form-section-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.form-section-head h3 { margin: 0; font-size: 13px; font-weight: 600; }
.form-section-head span { color: var(--mc-faint); font-size: 10.5px; text-align: right; }
.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.field-wide { grid-column: 1 / -1; }
.field > span { color: var(--mc-muted); font-size: 11.5px; font-weight: 500; }
.field em, .copyright-check em { color: var(--mc-accent); font-style: normal; }
.field input, .field select, .field textarea {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #3b3444;
  border-radius: 7px;
  outline: none;
  background: #0c0b10;
  color: var(--mc-text);
  font-size: 12.5px;
  line-height: 1.5;
}
.field textarea { resize: vertical; }
.field input::placeholder, .field textarea::placeholder { color: #726779; }
.field input:focus, .field select:focus, .field textarea:focus { border-color: rgba(232, 121, 249, .65); }
.field input[aria-invalid='true'], .field textarea[aria-invalid='true'] { border-color: var(--mc-negative); }
.field small { color: var(--mc-faint); font-size: 10.5px; line-height: 1.5; }
.file-meta-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 7px; color: var(--mc-faint); font-size: 10.5px; }
.verified-text { color: var(--mc-positive); }
.cover-editor { display: grid; grid-template-columns: minmax(0, 1fr) 122px; align-items: start; gap: 12px; }
.cover-large-preview { position: relative; display: flex; flex-direction: column; gap: 5px; }
.cover-large-preview img, .cover-placeholder { width: 122px; height: 122px; display: grid; place-items: center; border-radius: 8px; object-fit: cover; background: #1b1621; color: #6f637a; font-size: 24px; }
.cover-large-preview > span { color: var(--mc-faint); font-size: 10px; }
.lyric-field { margin-top: 10px; }
.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 38px; cursor: pointer; }
.switch-row > span { display: flex; flex-direction: column; gap: 3px; }
.switch-row strong { font-size: 12px; font-weight: 500; }
.switch-row small { color: var(--mc-faint); font-size: 10.5px; }
.switch-row input { width: 34px; height: 18px; accent-color: #e879f9; cursor: pointer; }
.copyright-section { padding: 13px; border: 1px solid #352d3f; border-radius: 8px; background: #141019; }
.copyright-check { display: flex; align-items: flex-start; gap: 10px; cursor: pointer; }
.copyright-check input { width: 15px; height: 15px; margin-top: 2px; flex: none; accent-color: #e879f9; }
.copyright-check span { display: flex; flex-direction: column; gap: 3px; }
.copyright-check strong { color: var(--mc-text); font-size: 11.5px; font-weight: 500; line-height: 1.5; }
.copyright-check small { color: var(--mc-faint); font-size: 10.5px; line-height: 1.55; }

.album-form .field-grid { margin-bottom: 16px; }
.album-cover-section { padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid #26212e; }
.album-tracks { display: flex; flex-direction: column; }
.track-search { display: flex; align-items: center; gap: 8px; height: 34px; padding: 0 9px; border: 1px solid #3b3444; border-radius: 7px; color: var(--mc-faint); background: #0c0b10; }
.track-search input { width: 100%; border: 0; outline: none; background: transparent; color: var(--mc-text); font-size: 12px; }
.track-options { max-height: 190px; margin-top: 7px; overflow-y: auto; }
.track-option { display: flex; align-items: center; gap: 9px; min-height: 44px; padding: 5px 6px; border-radius: 6px; cursor: pointer; }
.track-option:hover { background: #19151f; }
.track-option input { width: 14px; height: 14px; flex: none; accent-color: #e879f9; }
.track-option img { width: 30px; height: 30px; flex: none; border-radius: 4px; object-fit: cover; background: #211a2a; }
.track-option-copy { min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.track-option-copy strong { font-size: 11.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.track-option-copy small { color: var(--mc-faint); font-size: 10px; }
.track-empty { padding: 16px 8px; color: var(--mc-faint); font-size: 11.5px; text-align: center; }
.form-hint { margin: 7px 0 0; color: #807487; font-size: 10.5px; line-height: 1.5; }
.status-readout { display: flex; align-items: center; gap: 7px; min-height: 38px; }
.status-readout small { color: var(--mc-faint); font-size: 10.5px; }
.withdraw-method-field { margin: 0 0 14px; padding: 0; border: 0; }
.withdraw-method-field legend { padding: 0; margin-bottom: 7px; color: var(--mc-muted); font-size: 11.5px; font-weight: 500; }
.withdraw-method-field legend em { color: var(--mc-accent); font-style: normal; }
.withdraw-method-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.withdraw-method { position: relative; display: flex; align-items: center; gap: 8px; min-height: 52px; padding: 9px 10px; border: 1px solid #3b3444; border-radius: 8px; background: #141019; color: var(--mc-text); text-align: left; cursor: pointer; }
.withdraw-method:hover { border-color: #5c4d68; }
.withdraw-method.selected { border-color: rgba(232, 121, 249, .7); background: #21172a; }
.withdraw-method-logo { width: 27px; height: 27px; flex: none; display: grid; place-items: center; border-radius: 7px; color: #fff; font-size: 13px; font-weight: 700; }
.withdraw-method-logo.alipay { background: #1677ff; }
.withdraw-method-logo.wechat { background: #07c160; }
.withdraw-method-copy { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.withdraw-method-copy strong { font-size: 12px; font-weight: 600; }
.withdraw-method-copy small { color: var(--mc-faint); font-size: 10px; }
.withdraw-radio { position: absolute; top: 9px; right: 9px; width: 11px; height: 11px; border: 1px solid #5c5364; border-radius: 50%; }
.withdraw-method.selected .withdraw-radio { border: 3px solid var(--mc-accent); }
.withdraw-balance { display: flex; flex-direction: column; gap: 3px; padding: 12px 14px; margin-bottom: 14px; border: 1px solid #332c3c; border-radius: 8px; background: #141019; }
.withdraw-balance span { color: var(--mc-faint); font-size: 11px; }
.withdraw-balance strong { font-size: 25px; font-weight: 600; font-variant-numeric: tabular-nums; }
.withdraw-balance small { color: var(--mc-faint); font-size: 10.5px; }
.dialog-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.dialog-footer-actions { display: flex; align-items: center; gap: 6px; margin-left: auto; }

.singer-cert-layout { display: grid; grid-template-columns: minmax(0, .82fr) minmax(0, 1.18fr); gap: 14px; }
.singer-identity-card { display: flex; flex-direction: column; gap: 15px; }
.singer-profile-main { display: flex; align-items: center; gap: 13px; }
.singer-profile-avatar { flex: none; background: #241b2e; color: #e9b8ff; font-size: 24px; }
.singer-profile-name { display: block; font-size: 19px; font-weight: 600; letter-spacing: -.02em; }
.singer-profile-meta { display: block; margin-top: 4px; color: var(--mc-faint); font-size: 11px; }
.singer-profile-intro { margin: 0; color: var(--mc-muted); font-size: 12px; line-height: 1.65; }
.singer-rule { display: flex; align-items: flex-start; gap: 8px; margin-top: auto; padding: 11px 12px; border: 1px solid #352c3e; border-radius: 8px; color: var(--mc-faint); font-size: 11px; line-height: 1.55; }
.singer-rule .el-icon { flex: none; margin-top: 1px; color: var(--mc-accent); }
.singer-action-card { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.singer-action-card h3 { margin: 0; font-size: 17px; font-weight: 600; letter-spacing: -.02em; }
.singer-action-card > p { margin: 0; color: var(--mc-faint); font-size: 12px; line-height: 1.65; }
.singer-result-mark { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; font-size: 18px; font-weight: 700; }
.singer-result-mark.is-approved { color: #b7f36a; background: rgba(183, 243, 106, .1); border: 1px solid rgba(183, 243, 106, .3); }
.singer-result-mark.is-pending { color: #f7d774; background: rgba(247, 215, 116, .09); border: 1px solid rgba(247, 215, 116, .25); }
.singer-result-link { margin-top: 7px; }
.singer-submitted-at { color: var(--mc-faint); font-size: 10.5px; }
.singer-reject-note { display: flex; flex-direction: column; gap: 3px; width: 100%; padding: 10px 12px; border: 1px solid rgba(255, 158, 174, .25); border-radius: 7px; background: rgba(255, 158, 174, .06); color: #ff9eae; font-size: 11px; line-height: 1.55; }
.singer-reject-note strong { font-size: 11.5px; }
.singer-apply-form { width: 100%; }
.singer-apply-form .field-grid { margin-top: 2px; }
.singer-form-note, .singer-form-warning { margin: 10px 0 0; color: var(--mc-faint); font-size: 10.5px; line-height: 1.55; }
.singer-form-warning { color: #f7d774; }
.singer-submit { margin-top: 12px; align-self: flex-start; }
.singer-skeleton { height: 300px; border-radius: 9px; }
@media (max-width: 980px) { .singer-cert-layout { grid-template-columns: 1fr; } }
.mv-creator-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.mv-own-card { overflow: hidden; border: 1px solid var(--mc-border); border-radius: 10px; background: var(--mc-surface); }
.mv-own-cover { position: relative; aspect-ratio: 16 / 9; background: #17131d; }
.mv-own-cover img { width: 100%; height: 100%; display: block; object-fit: cover; }
.mv-own-placeholder { height: 100%; display: grid; place-items: center; color: #655b70; font-size: 24px; }
.mv-own-quality { position: absolute; top: 8px; right: 8px; padding: 2px 6px; border-radius: 4px; background: rgba(0, 0, 0, .6); color: var(--mc-accent); font-size: 9px; font-weight: 700; }
.mv-own-copy { display: flex; flex-direction: column; gap: 6px; padding: 10px 11px 11px; }
.mv-own-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.mv-own-head strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12.5px; font-weight: 600; }
.mv-own-head .state-pill { flex: none; height: 19px; padding: 0 6px; font-size: 9.5px; }
.mv-own-meta, .mv-own-foot { color: var(--mc-faint); font-size: 10.5px; }
.mv-own-foot { display: flex; gap: 3px; }
.mv-own-reject { margin: 1px 0 0; padding: 5px 6px; border-radius: 4px; background: rgba(255, 143, 162, .07); color: #ff9eae; font-size: 10px; line-height: 1.45; }
.mv-card-skeleton { height: 150px; border-radius: 10px; }
.mv-vip-field .mv-check { display: flex; align-items: center; gap: 7px; min-height: 38px; color: var(--mc-muted); font-size: 11.5px; }
.mv-check input { width: 14px; height: 14px; accent-color: var(--mc-accent); }
.mv-upload-section { padding-bottom: 14px; margin-bottom: 14px; }
.mv-publish-form .field-grid { margin-bottom: 14px; }
.mv-publish-form .mv-upload-section .form-section-head { margin-bottom: 8px; }
@media (max-width: 1100px) { .mv-creator-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .mv-creator-grid { grid-template-columns: 1fr; } }
@media (max-width: 1200px) {
  .stats-grid, .lower-grid, .income-grid { grid-template-columns: 1fr; }
  .metric-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .metric-cell:nth-child(3) { border-right: 0; }
  .metric-cell:nth-child(-n + 3) { border-bottom: 1px solid var(--mc-border); }
  .income-side { display: grid; grid-template-columns: 1fr 1fr; }
}
@media (max-width: 920px) {
  .release-grid { grid-template-columns: 1fr 1fr; }
  .release-card:nth-child(3) { margin-top: 14px; padding: 14px 0 0; border-left: 0; border-top: 1px solid #25202d; grid-column: 1 / -1; }

  .artist-actions { margin-left: 0; width: 100%; }
  .artist-band { flex-wrap: wrap; }
  .search-box { width: 240px; }
  .sort-select { margin-left: 0; }
  .album-grid { grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); }
}
@media (max-width: 760px) {
  .section-head { align-items: flex-start; }
  .section-note { display: none; }
  .artist-band { align-items: flex-start; padding: 12px; }
  .artist-avatar { width: 46px; height: 46px; }
  .artist-copy h1 { font-size: 22px; }
  .artist-actions { display: grid; grid-template-columns: 1fr 1fr; }
  .artist-actions .button { width: 100%; }
  .work-toolbar { align-items: stretch; }
  .search-box { width: 100%; }
  .filter-group { max-width: 100%; overflow-x: auto; scrollbar-width: none; }
  .filter-group::-webkit-scrollbar { display: none; }
  .sort-select { justify-content: space-between; width: 100%; }
  .sort-select select { flex: 1; }
  .bulk-bar { align-items: flex-start; flex-direction: column; gap: 8px; }
  .bulk-actions { width: 100%; flex-wrap: wrap; }
  .table-scroll { display: none; }
  .work-cards { display: flex; flex-direction: column; }
  .work-card { padding: 11px; border-bottom: 1px solid #1e1a24; }
  .work-card:last-child { border-bottom: 0; }
  .work-card.selected { background: #17131d; }
  .work-card-head { display: flex; align-items: center; gap: 9px; }
  .work-card-head input { width: 15px; height: 15px; flex: none; accent-color: #e879f9; }
  .work-card-head img { width: 42px; height: 42px; flex: none; border-radius: 5px; object-fit: cover; }
  .work-card-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 2px; }
  .work-card-copy strong { font-size: 12.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .work-card-copy span { color: var(--mc-faint); font-size: 11px; }
  .work-card-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0; margin: 10px 0 7px; padding: 8px 0; border-top: 1px solid #1e1a24; border-bottom: 1px solid #1e1a24; }
  .work-card-metrics div { display: flex; flex-direction: column; gap: 2px; padding-left: 9px; border-left: 1px solid #1e1a24; }
  .work-card-metrics div:first-child { border-left: 0; padding-left: 0; }
  .work-card-metrics dt { color: var(--mc-faint); font-size: 11px; }
  .work-card-metrics dd { margin: 0; color: var(--mc-text); font-size: 12px; font-variant-numeric: tabular-nums; }
  .work-card-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
  .work-card-foot > span { color: var(--mc-faint); font-size: 11px; }
  .table-footer { padding: 9px 11px; }
  .metric-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-cell:nth-child(2n) { border-right: 0; }
  .metric-cell:nth-child(3n) { border-right: 1px solid var(--mc-border); }
  .metric-cell:nth-child(-n + 4) { border-bottom: 1px solid var(--mc-border); }
  .metric-cell:nth-child(3) { border-right: 1px solid var(--mc-border); }
  .stats-head { flex-direction: column; align-items: flex-start; gap: 12px; }
  .period-switch { width: 100%; }
  .period-switch button { flex: 1; }
  .income-lead { flex-direction: column; align-items: flex-start; }
  .settlement-state { align-items: flex-start; }
  .balance-copy strong { font-size: 30px; }
  .income-side { display: flex; }
  .record-row { grid-template-columns: minmax(0, 1fr) 70px 92px; }
  .record-row time { display: none; }
  .lower-grid { margin-top: 10px; }
  .cover-editor { grid-template-columns: 1fr; }
  .cover-large-preview img, .cover-placeholder { width: 88px; height: 88px; }
  .field-grid { grid-template-columns: 1fr; }
  .field-wide { grid-column: auto; }
  .album-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }
  .album-card-body { padding: 9px; }
  .album-card-body p { min-height: 30px; }
  .album-meta { flex-direction: column; gap: 2px; }
}
@media (max-width: 520px) {
  .release-checks { padding: 14px; }
  .release-grid { grid-template-columns: 1fr; }
  .release-card { padding: 14px 0 0; margin-top: 14px; border-left: 0; border-top: 1px solid #25202d; }
  .release-card:first-child { padding-top: 0; margin-top: 0; border-top: 0; }
  .release-card:nth-child(3) { padding-top: 14px; }
  .release-head { flex-direction: column; gap: 4px; }

  .console-tabs { gap: 21px; }
  .console-tab { font-size: 13px; }
  .section-head h2 { font-size: 19px; }
  .section-head p { font-size: 11.5px; }
  .filter-chip { padding: 0 7px; font-size: 11px; }
  .filter-chip span { display: none; }
  .album-grid { grid-template-columns: 1fr 1fr; }
  .album-heading { align-items: flex-start; flex-direction: column; gap: 4px; }
  .album-heading .state-pill { align-self: flex-start; }
  .record-row { grid-template-columns: minmax(0, 1fr) 65px 76px; gap: 6px; }
  .row-actions .row-action { padding-left: 4px; padding-right: 4px; }
  .drawer-foot { padding: 10px 14px; }
  .drawer-foot .button { padding: 0 10px; font-size: 11.5px; }
}
</style>

<style>
.work-editor-drawer { background: #0d0b11 !important; border-left: 1px solid #2c2634; box-shadow: 0 0 0 1px rgba(0, 0, 0, .4), -24px 0 70px rgba(0, 0, 0, .42); /* taste-skill-ignore: floating drawer */ }
.work-editor-drawer .el-drawer__body { padding: 0 !important; display: flex; overflow: hidden; }
.drawer-shell { display: flex; flex-direction: column; width: 100%; height: 100%; color: #f7f4fc; }
.drawer-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 21px 24px 16px; border-bottom: 1px solid #2a2432; }
.drawer-head h2 { margin: 4px 0 3px; font-size: 22px; font-weight: 650; letter-spacing: -.03em; }
.drawer-head p { margin: 0; color: #948c9f; font-size: 11.5px; }
.drawer-close { height: 30px; padding: 0 9px; border: 1px solid #3b3444; border-radius: 6px; background: transparent; color: #b0a8bb; font-size: 11.5px; cursor: pointer; }
.drawer-close:hover { color: #fff; background: #19151f; }
.drawer-body { flex: 1; min-height: 0; padding: 20px 24px; overflow-y: auto; }
.drawer-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 13px 24px; border-top: 1px solid #2a2432; background: #100d15; }
.drawer-foot-actions { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.drawer-shell .editor-form { max-width: 100%; }
.album-editor-dialog, .withdraw-dialog { background: #0d0b11 !important; border: 1px solid #2c2634 !important; border-radius: 10px !important; }
.album-editor-dialog .el-dialog__header, .withdraw-dialog .el-dialog__header { padding: 18px 20px 10px; margin: 0; border-bottom: 1px solid #25202d; }
.album-editor-dialog .el-dialog__title, .withdraw-dialog .el-dialog__title { color: #f7f4fc; font-size: 16px; font-weight: 600; }
.album-editor-dialog .el-dialog__body, .withdraw-dialog .el-dialog__body { padding: 18px 20px; color: #f7f4fc; }
.album-editor-dialog .el-dialog__footer, .withdraw-dialog .el-dialog__footer { padding: 0 20px 18px; }
.album-editor-dialog .el-dialog__headerbtn .el-dialog__close, .withdraw-dialog .el-dialog__headerbtn .el-dialog__close { color: #9c92a7; }
.album-editor-dialog .el-dialog__headerbtn:hover .el-dialog__close, .withdraw-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #fff; }
@media (max-width: 520px) {
  .withdraw-method-grid { grid-template-columns: 1fr; }
}

@media (max-width: 720px) {
  .work-editor-drawer { width: 100% !important; }
  .drawer-head { padding: 18px 16px 14px; }
  .drawer-body { padding: 16px; }
  .drawer-foot { padding: 11px 16px; }
  .album-editor-dialog, .withdraw-dialog { width: calc(100vw - 24px) !important; margin-top: 5vh; }
}
</style>



<style>
.mv-publish-dialog {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  margin-top: 5vh !important;
  background: #0d0b11 !important;
  border: 1px solid #2c2634 !important;
  border-radius: 10px !important;
}
.mv-publish-dialog .el-dialog__header { padding: 18px 20px 10px; margin: 0; border-bottom: 1px solid #25202d; }
.mv-publish-dialog .el-dialog__title { color: #f7f4fc; font-size: 16px; font-weight: 600; }
.mv-publish-dialog .el-dialog__body { flex: 1; min-height: 0; padding: 16px 20px; overflow-y: auto; color: #f7f4fc; }
.mv-publish-dialog .el-dialog__footer { flex: none; padding: 0 20px 18px; }
.mv-publish-dialog .el-dialog__headerbtn .el-dialog__close { color: #9c92a7; }
.mv-publish-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #fff; }
@media (max-height: 760px) { .mv-publish-dialog { max-height: 94vh; margin-top: 3vh !important; } }
.album-editor-dialog {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  margin-top: 5vh !important;
}
.album-editor-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.album-editor-dialog .el-dialog__footer {
  flex: none;
}
@media (max-height: 760px) {
  .album-editor-dialog { max-height: 94vh; margin-top: 3vh !important; }
}
</style>




