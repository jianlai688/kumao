<template>
  <svg
    class="km-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <template v-if="name === 'compass'">
      <circle cx="12" cy="12" r="9" />
      <path d="m15.7 8.3-2 5.4-5.4 2 2-5.4 5.4-2Z" />
    </template>
    <template v-else-if="name === 'rank'">
      <path d="M5 19v-4M12 19V9M19 19V5" />
      <path d="M3.5 19h17" opacity=".45" />
    </template>
    <template v-else-if="name === 'user'">
      <circle cx="12" cy="8" r="3.25" />
      <path d="M5.5 19c.7-3.1 3.1-4.8 6.5-4.8s5.8 1.7 6.5 4.8" />
    </template>
    <template v-else-if="name === 'library'">
      <path d="M8.5 18V6.8L18 4.6v10.8" />
      <circle cx="6" cy="18" r="2.5" />
      <circle cx="15.5" cy="15.4" r="2.5" />
    </template>
    <template v-else-if="name === 'playlist'">
      <rect x="4" y="4" width="16" height="16" rx="4" />
      <path d="M8 9h8M8 12.5h8M8 16h4.5" />
      <path d="M17 14.5v4" />
    </template>
    <template v-else-if="name === 'mv'">
      <rect x="3.5" y="6" width="12.5" height="12" rx="3" />
      <path d="m16 10.4 4-2.2v7.6l-4-2.2" />
      <path d="m9 9.8 3 2.2-3 2.2V9.8Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'live'">
      <circle cx="12" cy="12" r="9" />
      <path d="m10 8.6 5 3.4-5 3.4V8.6Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'mic'">
      <rect x="9" y="3.5" width="6" height="11" rx="3" />
      <path d="M6.5 11.5a5.5 5.5 0 0 0 11 0M12 17v3.5M9.5 20.5h5" />
    </template>
    <template v-else-if="name === 'music'">
      <path d="M9 18V6.5L19 4v11.5" />
      <circle cx="6.5" cy="18" r="2.5" />
      <circle cx="16.5" cy="15.5" r="2.5" />
    </template>
    <template v-else-if="name === 'crown'">
      <path d="m4 7 4.2 3.2L12 5l3.8 5.2L20 7l-1.5 10h-13L4 7Z" />
      <path d="M6.5 20h11" />
    </template>
    <template v-else-if="name === 'search'">
      <circle cx="10.8" cy="10.8" r="6.3" />
      <path d="m15.5 15.5 4.2 4.2" />
    </template>
    <template v-else-if="name === 'back'">
      <path d="m14.5 5.5-6 6.5 6 6.5" />
    </template>
    <template v-else-if="name === 'forward'">
      <path d="m9.5 5.5 6 6.5-6 6.5" />
    </template>
    <template v-else-if="name === 'chevron-down'">
      <path d="m6.5 9.5 5.5 5 5.5-5" />
    </template>
    <template v-else-if="name === 'chevron-right'">
      <path d="m9.5 6 6 6-6 6" />
    </template>
    <template v-else-if="name === 'upload'">
      <path d="M12 16V4.5M7.5 8.5 12 4l4.5 4.5" />
      <path d="M5 15v3.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V15" />
    </template>
    <template v-else-if="name === 'sun'">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4" />
    </template>
    <template v-else-if="name === 'bell'">
      <path d="M6.8 10.2a5.2 5.2 0 0 1 10.4 0v3.2l1.5 2.4H5.3l1.5-2.4v-3.2Z" />
      <path d="M10 18.5a2.2 2.2 0 0 0 4 0" />
    </template>
    <template v-else-if="name === 'play'">
      <path d="m8.5 5.8 10 6.2-10 6.2V5.8Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'pause'">
      <rect x="7" y="5" width="3.5" height="14" rx="1.2" fill="currentColor" stroke="none" />
      <rect x="13.5" y="5" width="3.5" height="14" rx="1.2" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'heart'">
      <path
        d="M12 20s-7.4-4.3-7.4-9.4A4.1 4.1 0 0 1 12 7.6a4.1 4.1 0 0 1 7.4 3C19.4 15.7 12 20 12 20Z"
        :fill="filled ? 'currentColor' : 'none'"
      />
    </template>
    <template v-else-if="name === 'more'">
      <circle cx="5" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="12" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="19" cy="12" r="1.3" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'shuffle'">
      <path d="M4 7h3.2c4 0 5.6 10 9.6 10H20" />
      <path d="m17 14 3 3-3 3M4 17h3.2c1.2 0 2.2-.9 3.1-2.2M13.7 7.2C14.6 7 15.4 7 16.8 7H20" />
      <path d="m17 4 3 3-3 3" />
    </template>
    <template v-else-if="name === 'prev'">
      <path d="M6.5 5v14" />
      <path d="M18 6.8v10.4L9.5 12 18 6.8Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'next'">
      <path d="M17.5 5v14" />
      <path d="M6 6.8v10.4L14.5 12 6 6.8Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'repeat'">
      <path d="M17 4.5 20 7l-3 2.5" />
      <path d="M19 7H8a4 4 0 0 0-4 4" />
      <path d="M7 19.5 4 17l3-2.5" />
      <path d="M5 17h11a4 4 0 0 0 4-4" />
    </template>
    <template v-else-if="name === 'repeat-one'">
      <path d="M17 4.5 20 7l-3 2.5" />
      <path d="M19 7H8a4 4 0 0 0-4 4" />
      <path d="M7 19.5 4 17l3-2.5" />
      <path d="M5 17h11a4 4 0 0 0 4-4" />
      <path d="M11.2 10.4 12.8 9.4v5.1" />
      <path d="M11 14.5h3.6" />
    </template>
    <template v-else-if="name === 'order'">
      <path d="M4 6.5h8M4 12h8M4 17.5h5" />
      <path d="M17 8.5v8M14.2 13.6 17 16.4l2.8-2.8" />
    </template>
    <template v-else-if="name === 'queue'">
      <rect x="4" y="5" width="4" height="4" rx="1" />
      <rect x="4" y="15" width="4" height="4" rx="1" />
      <path d="M11 7h9M11 17h9" />
    </template>
    <template v-else-if="name === 'lyrics'">
      <rect x="4" y="4.5" width="16" height="15" rx="3" />
      <path d="M8 9h8M8 12.5h5M8 16h7" />
    </template>
    <template v-else-if="name === 'volume'">
      <path d="M5 10v4h3l4 3.3V6.7L8 10H5Z" />
      <path d="M15 9a4 4 0 0 1 0 6M17.5 6.8a7.2 7.2 0 0 1 0 10.4" />
    </template>
    <template v-else-if="name === 'star'">
      <path d="m12 3.7 2.35 4.76 5.25.77-3.8 3.7.9 5.23L12 15.7l-4.7 2.46.9-5.23-3.8-3.7 5.25-.77L12 3.7Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'sparkle'">
      <path d="M12 3.5c.5 3.6 2.4 5.5 6 6-3.6.5-5.5 2.4-6 6-.5-3.6-2.4-5.5-6-6 3.6-.5 5.5-2.4 6-6Z" fill="currentColor" stroke="none" />
      <path d="M18.5 15.5c.25 1.6 1.15 2.5 2.75 2.75-1.6.25-2.5 1.15-2.75 2.75-.25-1.6-1.15-2.5-2.75-2.75 1.6-.25 2.5-1.15 2.75-2.75Z" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'list'">
      <path d="M8.5 6h11M8.5 12h11M8.5 18h11" />
      <circle cx="4.5" cy="6" r="1" fill="currentColor" stroke="none" />
      <circle cx="4.5" cy="12" r="1" fill="currentColor" stroke="none" />
      <circle cx="4.5" cy="18" r="1" fill="currentColor" stroke="none" />
    </template>
  </svg>
</template>

<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  strokeWidth: { type: [Number, String], default: 1.8 },
  filled: { type: Boolean, default: false }
})
</script>

<style scoped>
.km-icon {
  display: block;
  flex: none;
  overflow: visible;
}
</style>
